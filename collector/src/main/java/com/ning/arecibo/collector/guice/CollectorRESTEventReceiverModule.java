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

package com.ning.arecibo.collector.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.receiver.RESTEventReceiverModule;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.util.Logger;
import org.skife.config.ConfigurationObjectFactory;

import java.util.ArrayList;
import java.util.List;

public class CollectorRESTEventReceiverModule extends AbstractModule
{
    private static final Logger log = Logger.getLogger(CollectorRESTEventReceiverModule.class);

    private final List<Class<? extends EventSerializer>> serializers = new ArrayList<Class<? extends EventSerializer>>();

    @Override
    protected void configure()
    {
        final CollectorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(CollectorConfig.class);

        for (final String eventSerializer : config.getEventSerializers().split(",")) {
            try {
                serializers.add(Class.forName(eventSerializer).asSubclass(EventSerializer.class));
            }
            catch (ClassNotFoundException e) {
                log.warn("Unable to find class " + eventSerializer, e);
            }
            catch (ClassCastException e) {
                log.warn("Unable to cast class " + eventSerializer, e);
            }
        }

        install(new RESTEventReceiverModule(CollectorEventProcessor.class, "arecibo.collector:name=CollectorEventProcessor", serializers));
    }
}
