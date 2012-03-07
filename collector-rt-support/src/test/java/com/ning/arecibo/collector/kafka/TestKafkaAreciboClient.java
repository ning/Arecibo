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

package com.ning.arecibo.collector.kafka;

import com.ning.arecibo.collector.RealtimeClient;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.eventlogger.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.UUID;

public class TestKafkaAreciboClient
{
    // ByteBuffer payload for:
    // {"timestamp":1331148118082,"min_heapUsed":1.515698888E9,"max_heapUsed":1.835511784E9,"eventType":"fcc83090-a6d0-4509-8a0a-3a06b99f96f7","sourceUUID":"714be299-b735-4663-8efe-74f10dc62a7a"}
    private static final byte[] payload = {123, 34, 116, 105, 109, 101, 115, 116, 97, 109, 112, 34, 58, 49, 51, 51, 49, 49,
        52, 56, 49, 49, 56, 48, 56, 50, 44, 34, 109, 105, 110, 95, 104, 101, 97, 112, 85, 115, 101, 100, 34, 58, 49, 46, 53,
        49, 53, 54, 57, 56, 56, 56, 56, 69, 57, 44, 34, 109, 97, 120, 95, 104, 101, 97, 112, 85, 115, 101, 100, 34, 58, 49, 46,
        56, 51, 53, 53, 49, 49, 55, 56, 52, 69, 57, 44, 34, 101, 118, 101, 110, 116, 84, 121, 112, 101, 34, 58, 34, 102, 99, 99,
        56, 51, 48, 57, 48, 45, 97, 54, 100, 48, 45, 52, 53, 48, 57, 45, 56, 97, 48, 97, 45, 51, 97, 48, 54, 98, 57, 57, 102,
        57, 54, 102, 55, 34, 44, 34, 115, 111, 117, 114, 99, 101, 85, 85, 73, 68, 34, 58, 34, 55, 49, 52, 98, 101, 50, 57, 57, 45,
        98, 55, 51, 53, 45, 52, 54, 54, 51, 45, 56, 101, 102, 101, 45, 55, 52, 102, 49, 48, 100, 99, 54, 50, 97, 55, 97, 34, 125};

    RealtimeClient client;

    @Test(groups = "fast")
    public void testParser() throws Exception
    {
        final Event event = KafkaAreciboClient.convertByteBufferPayloadToEvent(payload);
        Assert.assertEquals(event.getEventType(), "fcc83090-a6d0-4509-8a0a-3a06b99f96f7");
        Assert.assertEquals(event.getTimestamp(), 1331148118082L);
        Assert.assertEquals(event.getSourceUUID(), UUID.fromString("714be299-b735-4663-8efe-74f10dc62a7a"));

        // We convert only to MapEvent for now
        Assert.assertTrue(event instanceof MapEvent);
        final MapEvent mapEvent = (MapEvent) event;
        Assert.assertEquals(Double.valueOf(mapEvent.getMap().get("min_heapUsed").toString()), 1.515698888E9);
        Assert.assertEquals(Double.valueOf(mapEvent.getMap().get("max_heapUsed").toString()), 1.835511784E9);
    }
}