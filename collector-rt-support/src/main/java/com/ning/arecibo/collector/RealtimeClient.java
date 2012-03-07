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

package com.ning.arecibo.collector;

import com.google.common.base.Function;
import com.ning.arecibo.eventlogger.Event;

/**
 * Listener to streams of events
 *
 * @param <F> Raw event type from the realtime system, e.g. kafka.message.Message or javax.jms.Message
 */
public interface RealtimeClient<F>
{
    /**
     * Register a listener to a stream. The behavior of calling listenToStream multiple times without calling stopListening
     * first is undefined.
     *
     * @param topic              Topic to listen to (e.g. Kafka topic or AMQ queue)
     * @param onValidMessage     handler for successfully parsed messages
     * @param onCorruptedMessage handler for invalid messages (e.g. events with bad checksums)
     */
    void listenToStream(final String topic, final Function<Event, Void> onValidMessage, final Function<F, Void> onCorruptedMessage);

    /**
     * Shut down a listener. Calling the method multiple times has no effect.
     *
     * @param topic Topic to stop listening to (e.g. Kafka topic or AMQ queue)
     */
    void stopListening(final String topic);
}
