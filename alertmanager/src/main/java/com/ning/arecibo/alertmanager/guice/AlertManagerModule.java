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

package com.ning.arecibo.alertmanager.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.alert.client.AlertClient;
import com.ning.arecibo.alert.client.AlertClientConfig;
import com.ning.arecibo.alert.client.discovery.AlertFinder;
import com.ning.arecibo.alert.client.discovery.DefaultAlertFinder;
import com.ning.arecibo.alert.client.rest.DefaultAlertClient;
import org.skife.config.ConfigurationObjectFactory;

public class AlertManagerModule extends AbstractModule
{
    @Override
    public void configure()
    {
        final AlertClientConfig alertclientconfig = new ConfigurationObjectFactory(System.getProperties()).build(AlertClientConfig.class);
        bind(AlertClientConfig.class).toInstance(alertclientconfig);

        // TODO hook ServiceLocator
        final AlertFinder alertFinder = new DefaultAlertFinder(alertclientconfig);
        bind(AlertFinder.class).toInstance(alertFinder);

        bind(AlertClient.class).to(DefaultAlertClient.class).asEagerSingleton();
    }
}
