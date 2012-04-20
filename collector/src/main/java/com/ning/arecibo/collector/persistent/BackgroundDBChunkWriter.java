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
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.weakref.jmx.Managed;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mogwee.executors.Executors;
import com.ning.arecibo.collector.guice.CollectorConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.TimelineChunk;
import com.ning.arecibo.util.timeline.TimelineDAO;

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

    private final AtomicLong maybePerformBackgroundWritesCount = new AtomicLong();
    private final AtomicLong backgroundWritesCount = new AtomicLong();
    private final AtomicLong pendingChunkMapsAdded = new AtomicLong();
    private final AtomicLong pendingChunksAdded = new AtomicLong();
    private final AtomicLong pendingChunkMapsWritten = new AtomicLong();
    private final AtomicLong pendingChunksWritten = new AtomicLong();
    private final AtomicLong pendingChunkMapsMarkedConsumed = new AtomicLong();
    private final AtomicLong foregroundChunkMapsWritten = new AtomicLong();
    private final AtomicLong foregroundChunksWritten = new AtomicLong();

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

    public synchronized void addPendingChunkMap(final PendingChunkMap chunkMap)
    {
        if (shuttingDown.get()) {
            log.error("In addPendingChunkMap(), but finishBackgroundWritingAndExit is true!");
        }
        else {
            if (performForegroundWrites) {
                foregroundChunkMapsWritten.incrementAndGet();
                final List<TimelineChunk> chunksToWrite = new ArrayList<TimelineChunk>(chunkMap.getChunkMap().values());
                foregroundChunksWritten.addAndGet(chunksToWrite.size());
                timelineDAO.bulkInsertTimelineChunks(chunksToWrite);
                chunkMap.getAccumulator().markPendingChunkMapConsumed(chunkMap.getPendingChunkMapId());
            }
            else {
                pendingChunkMapsAdded.incrementAndGet();
                final int chunkCount = chunkMap.getChunkCount();
                pendingChunksAdded.addAndGet(chunkCount);
                pendingChunks.add(chunkMap);
                pendingChunkCount.addAndGet(chunkCount);
            }
        }
    }

    private void performBackgroundWrites()
    {
        backgroundWritesCount.incrementAndGet();
        List<PendingChunkMap> chunkMapsToWrite = null;
        synchronized(this) {
            chunkMapsToWrite = pendingChunks;
            pendingChunks = new ArrayList<PendingChunkMap>();
            pendingChunkCount.set(0);
        }
        final List<TimelineChunk> chunks = new ArrayList<TimelineChunk>();
        for (PendingChunkMap map : chunkMapsToWrite) {
            pendingChunkMapsWritten.incrementAndGet();
            pendingChunksWritten.addAndGet(map.getChunkMap().size());
            chunks.addAll(map.getChunkMap().values());
        }
        timelineDAO.bulkInsertTimelineChunks(chunks);
        for (PendingChunkMap map : chunkMapsToWrite) {
            pendingChunkMapsMarkedConsumed.incrementAndGet();
            map.getAccumulator().markPendingChunkMapConsumed(map.getPendingChunkMapId());
        }
    }

    private void maybePerformBackgroundWrites()
    {
        // If already running background writes, just return
        maybePerformBackgroundWritesCount.incrementAndGet();
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

    @Managed
    public long getMaybePerformBackgroundWritesCount()
    {
    return maybePerformBackgroundWritesCount.get();
    }

    @Managed
    public long getBackgroundWritesCount()
    {
    return backgroundWritesCount.get();
    }

    @Managed
    public long getPendingChunkMapsAdded()
    {
    return pendingChunkMapsAdded.get();
    }

    @Managed
    public long getPendingChunksAdded()
    {
    return pendingChunksAdded.get();
    }

    @Managed
    public long getPendingChunkMapsWritten()
    {
    return pendingChunkMapsWritten.get();
    }

    @Managed
    public long getPendingChunksWritten()
    {
    return pendingChunksWritten.get();
    }

    @Managed
    public long getPendingChunkMapsMarkedConsumed()
    {
    return pendingChunkMapsMarkedConsumed.get();
    }

    @Managed
    public long getForegroundChunkMapsWritten()
    {
    return foregroundChunkMapsWritten.get();
    }

    @Managed
    public long getForegroundChunksWritten()
    {
        return foregroundChunksWritten.get();
    }
}
