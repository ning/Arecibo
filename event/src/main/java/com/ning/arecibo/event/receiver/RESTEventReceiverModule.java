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

package com.ning.arecibo.event.receiver;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.event.transport.JsonEventSerializer;
import com.ning.arecibo.event.transport.MapEventSerializer;
import com.ning.arecibo.util.ArrayListProvider;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import java.util.List;

public class RESTEventReceiverModule extends AbstractModule
{
    private static final List<Class<? extends EventSerializer>> DEFAULT_SERIALIZERS = ImmutableList.of(JavaEventSerializer.class, JsonEventSerializer.class, MapEventSerializer.class);

    private final Class<? extends BaseEventProcessor> clazz;
    private final String jmxObjectName;
    private final List<Class<? extends EventSerializer>> serializers;

    public RESTEventReceiverModule(final Class<? extends BaseEventProcessor> clazz, final String jmxObjectName)
    {
        this(clazz, jmxObjectName, DEFAULT_SERIALIZERS);
    }

    public RESTEventReceiverModule(final Class<? extends BaseEventProcessor> clazz, final String jmxObjectName, final List<Class<? extends EventSerializer>> serializers)
    {
        this.clazz = clazz;
        this.jmxObjectName = jmxObjectName;
        this.serializers = serializers;
    }

    @Override
    public void configure()
    {
        bind(clazz).asEagerSingleton();
        bind(BaseEventProcessor.class).to(clazz).asEagerSingleton();

        final ArrayListProvider<EventSerializer> eventSerializerProvider = new ArrayListProvider<EventSerializer>();
        for (final Class<? extends EventSerializer> serializer : serializers) {
            eventSerializerProvider.add(serializer);
        }

        bind(new TypeLiteral<List<EventSerializer>>()
        {
        }).annotatedWith(EventSerializers.class).toProvider(eventSerializerProvider);

        bind(EventParser.class).asEagerSingleton();
        bind(RESTEventEndPoint.class).asEagerSingleton();

        final ExportBuilder builder = MBeanModule.newExporter(binder());
        builder.export(BaseEventProcessor.class).as(jmxObjectName);
    }
}
