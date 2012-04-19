/*
 * Copyright 2010-2012 Ning, Inc.
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Function;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import com.ning.arecibo.util.timeline.persistent.Replayer;

// TODO: Unfinished
public class TimelineEventLoadGenerator {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
    private static final String REPLAY_FILE_DIRECTORY = System.getProperty("arecibo.collector.timeline.replayFileDirectory");
    private static final int REPLAY_REPEAT_COUNT = Integer.parseInt(System.getProperty("arecibo.collector.timeline.replayRepeatCount"));

    private final Map<Integer, DateTime> latestHostTimes = new HashMap<Integer, DateTime>();

    private int millisecondsToAdd = 0;

    private void generateEventStream()
    {
        final Replayer replayer = new Replayer(REPLAY_FILE_DIRECTORY);
        final DateTime startTime = new DateTime();
        for (int i=0; i<REPLAY_REPEAT_COUNT; i++) {
            final Collection<File> files = FileUtils.listFiles(new File(REPLAY_FILE_DIRECTORY), new String[]{"bin"}, false);

            for (final File file : Replayer.FILE_ORDERING.sortedCopy(files)) {
                try {
                    replayer.read(file, new Function<HostSamplesForTimestamp, Void>() {

                        @Override
                        public Void apply(HostSamplesForTimestamp hostSamples) {
                            processSamples(hostSamples);
                            return null;
                        }
                    });

                }
                catch (IOException e) {
                    log.warn("Exception replaying file: {}", file.getAbsolutePath(), e);
                }
            }
            latestHostTimes.clear();
            millisecondsToAdd = (int)(System.currentTimeMillis() - startTime.getMillis());
        }
    }

    private void processSamples(final HostSamplesForTimestamp hostSamples)
    {
        final DateTime timestamp = hostSamples.getTimestamp();
        final int hostId = hostSamples.getHostId();
        DateTime latestTime = latestHostTimes.get(hostId);
        if (latestTime == null) {
            latestTime = timestamp.plusMillis(millisecondsToAdd);
            latestHostTimes.put(hostId, latestTime);
        }
        if (timestamp.isBefore(latestTime)) {
            log.warn("Generating samples for host {}, timestamp {} is earlier than the end time {}; ignored",
                new Object[]{hostId, dateFormatter.print(timestamp), dateFormatter.print(latestTime)});
        }
        else {
            final DateTime newTimestamp = latestTime;
            final HostSamplesForTimestamp newSamples = new HostSamplesForTimestamp(hostId, hostSamples.getCategory(), newTimestamp, hostSamples.getSamples());
            // TODO: Send the samples to the collector
        }
    }

    public static void main(String[] args)
    {
        new TimelineEventLoadGenerator().generateEventStream();
    }
}
