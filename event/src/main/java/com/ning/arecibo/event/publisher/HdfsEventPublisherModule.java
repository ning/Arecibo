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

package com.ning.arecibo.event.publisher;

import java.util.concurrent.ExecutorService;

import com.mogwee.executors.FailsafeScheduledExecutor;
import com.mogwee.executors.NamedThreadFactory;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.RandomSelector;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceSelector;

public class HdfsEventPublisherModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(HdfsEventPublisherModule.class);

	private final String senderType;
	private final String serviceName;

	public HdfsEventPublisherModule(String senderType, String serviceName)
	{
		this.senderType = senderType;
		this.serviceName = serviceName;
	}

	@Override
	public void configure()
	{
		bind(ExecutorService.class).annotatedWith(PublisherExecutor.class).toInstance(
			new FailsafeScheduledExecutor(50, "EventPublisher")
		);

        bind(Selector.class).annotatedWith(RandomSelector.class).toInstance(new ServiceSelector(serviceName));

        EventPublisherConfig config = new ConfigurationObjectFactory(System.getProperties()).build(EventPublisherConfig.class);

        bind(EventPublisherConfig.class).toInstance(config);
        bind(EventPublisher.class).to(HdfsEventPublisher.class).asEagerSingleton();
        bind(String.class).annotatedWith(EventSenderType.class).toInstance(senderType);
        bind(EventServiceChooser.class).to(RandomEventServiceChooser.class).asEagerSingleton();
        bind(HdfsEventPublisher.class).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(RandomEventServiceChooser.class).as("arecibo:type=HdfsEventServiceChooser");
        builder.export(HdfsEventPublisher.class).as("arecibo:name=HdfsEventPublisher");
	}
}
