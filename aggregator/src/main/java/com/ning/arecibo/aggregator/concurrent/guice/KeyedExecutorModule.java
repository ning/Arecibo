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

package com.ning.arecibo.aggregator.concurrent.guice;

import java.util.concurrent.ExecutorService;
import org.skife.config.ConfigurationObjectFactory;
import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;
import com.google.inject.AbstractModule;
import com.ning.arecibo.aggregator.concurrent.KeyedExecutor;

public class KeyedExecutorModule extends AbstractModule
{
    @Override
	public void configure()
	{
        KeyedExecutorConfig config = new ConfigurationObjectFactory(System.getProperties()).build(KeyedExecutorConfig.class);

        bind(KeyedExecutorConfig.class).toInstance(config);
        bind(ExecutorService.class).annotatedWith(KeyedExecutorSupport.class).toProvider(KeyedExecutorThreadPoolProvider.class).asEagerSingleton();
	    bind(KeyedExecutor.class).asEagerSingleton();

	    ExportBuilder builder = MBeanModule.newExporter(binder());

        builder.export(KeyedExecutor.class).as("arecibo.concurrent:name=KeyedExecutor");
    }
}
