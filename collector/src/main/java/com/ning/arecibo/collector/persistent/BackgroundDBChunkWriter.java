/*
x * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.collector.persistent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineDAO;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.CounterMetric;

/**
 * This class runs a thread that batch-writes TimelineChunks to the db.
 * This class is thread-safe, and only holds up threads that want to queue
 * TimelineChunks for the time it takes to copy the ArrayList of PendingChunkMaps.
 * <p/>
 * The background writing thread is scheduled every few seconds, as controlled by
 * config.getBackgroundWriteCheckInterval().  It writes the current inventory of
 * chunks if there are at least config.getBackgroundWriteBatchSize()
 * TimelineChunks to be written, or if the time since the last write exceeds
 * config.getBackgroundWriteMaxDelay().
 */
@Singleton
public class BackgroundDBChunkWriter
{
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    private final TimelineDAO timelineDAO;
    private final CollectorConfig config;
    private final boolean performForegroundWrites;

    private final AtomicInteger pendingChunkCount = new AtomicInteger();
    private final AtomicBoolean shuttingDown = new AtomicBoolean();
    private List<PendingChunkMap> pendingChunks = new ArrayList<PendingChunkMap>();
    private DateTime lastWriteTime = new DateTime();
    private AtomicBoolean doingWritesNow = new AtomicBoolean();
    private final ScheduledExecutorService backgroundWriteThread = Executors.newSingleThreadScheduledExecutor("TimelineCommitter");

    private Map<String, CounterMetric> backgroundWriteCounters = new HashMap<String, CounterMetric>();

    private final CounterMetric maybePerformBackgroundWritesCount = makeCounter("maybePerformBackgroundWritesCount");
    private final CounterMetric backgroundWritesCount = makeCounter("backgroundWritesCount");
    private final CounterMetric pendingChunkMapsAdded = makeCounter("pendingChunkMapsAdded");
    private final CounterMetric pendingChunksAdded = makeCounter("pendingChunksAdded");
    private final CounterMetric pendingChunkMapsWritten = makeCounter("pendingChunksMapsWritten");
    private final CounterMetric pendingChunksWritten = makeCounter("pendingChunksWritten");
    private final CounterMetric pendingChunkMapsMarkedConsumed = makeCounter("pendingChunkMapsMarkedConsumed");
    private final CounterMetric foregroundChunkMapsWritten = makeCounter("foregroundChunkMapsWritten");
    private final CounterMetric foregroundChunksWritten = makeCounter("foregroundChunksWritten");

    @Inject
    public BackgroundDBChunkWriter(final TimelineDAO timelineDAO, final CollectorConfig config)
    {
        this(timelineDAO, config, config.getPerformForegroundWrites());
    }

    public BackgroundDBChunkWriter(final TimelineDAO timelineDAO, final @Nullable CollectorConfig config, final boolean performForegroundWrites)
    {
        this.timelineDAO = timelineDAO;
        this.config = config;
        this.performForegroundWrites = performForegroundWrites;
    }

    private CounterMetric makeCounter(final String counterName)
    {
        final CounterMetric counter = Metrics.newCounter(BackgroundDBChunkWriter.class, counterName);
        backgroundWriteCounters.put(counterName, counter);
        return counter;
    }

    public synchronized void addPendingChunkMap(final PendingChunkMap chunkMap)
    {
        if (shuttingDown.get()) {
            log.error("In addPendingChunkMap(), but finishBackgroundWritingAndExit is true!");
        }
        else {
            if (performForegroundWrites) {
                foregroundChunkMapsWritten.inc();
                final List<TimelineChunk> chunksToWrite = new ArrayList<TimelineChunk>(chunkMap.getChunkMap().values());
                foregroundChunksWritten.inc(chunksToWrite.size());
                timelineDAO.bulkInsertTimelineChunks(chunksToWrite);
                chunkMap.getAccumulator().markPendingChunkMapConsumed(chunkMap.getPendingChunkMapId());
            }
            else {
                pendingChunkMapsAdded.inc();
                final int chunkCount = chunkMap.getChunkCount();
                pendingChunksAdded.inc(chunkCount);
                pendingChunks.add(chunkMap);
                pendingChunkCount.addAndGet(chunkCount);
            }
        }
    }

    private void performBackgroundWrites()
    {
        backgroundWritesCount.inc();
        List<PendingChunkMap> chunkMapsToWrite = null;
        synchronized(this) {
            chunkMapsToWrite = pendingChunks;
            pendingChunks = new ArrayList<PendingChunkMap>();
            pendingChunkCount.set(0);
        }
        final List<TimelineChunk> chunks = new ArrayList<TimelineChunk>();
        for (PendingChunkMap map : chunkMapsToWrite) {
            pendingChunkMapsWritten.inc();
            pendingChunksWritten.inc(map.getChunkMap().size());
            chunks.addAll(map.getChunkMap().values());
        }
        timelineDAO.bulkInsertTimelineChunks(chunks);
        for (PendingChunkMap map : chunkMapsToWrite) {
            pendingChunkMapsMarkedConsumed.inc();
            map.getAccumulator().markPendingChunkMapConsumed(map.getPendingChunkMapId());
        }
    }

    private void maybePerformBackgroundWrites()
    {
        // If already running background writes, just return
        maybePerformBackgroundWritesCount.inc();
        if (!doingWritesNow.compareAndSet(false, true)) {
            return;
        }
        else {
            try {
                if (shuttingDown.get()) {
                    performBackgroundWrites();
                }
                final int pendingCount = pendingChunkCount.get();
                if (pendingCount > 0) {
                    if (pendingCount >= config.getBackgroundWriteBatchSize() ||
                            new DateTime().isBefore(lastWriteTime.plusMillis((int)config.getBackgroundWriteMaxDelay().getMillis()))) {
                        performBackgroundWrites();
                        lastWriteTime = new DateTime();
                    }
                }
            }
            finally {
                doingWritesNow.set(false);
            }
        }
    }

    public synchronized boolean getShutdownFinished()
    {
        return doingWritesNow.get() == false && pendingChunks.size() == 0;
    }

    public void initiateShutdown()
    {
        shuttingDown.set(true);
    }

    public void runBackgroundWriteThread() {
        if (!performForegroundWrites) {
            backgroundWriteThread.scheduleWithFixedDelay(new Runnable()
            {
                @Override
                public void run()
                {
                    maybePerformBackgroundWrites();
                }
            },
            config.getBackgroundWriteCheckInterval().getMillis(),
            config.getBackgroundWriteCheckInterval().getMillis(),
            TimeUnit.MILLISECONDS);
        }
    }

    public void stopBackgroundWriteThread() {
        if (!performForegroundWrites) {
            backgroundWriteThread.shutdown();
        }
    }
}
