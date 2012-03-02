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

package com.ning.arecibo.util.service;

import com.google.inject.Inject;

public class RandomServiceChooser implements ServiceChooser
{
    private final ServiceLocator serviceLocator;
    private final Selector selector;

    @Inject
    public RandomServiceChooser(ServiceLocator serviceLocator, @RandomSelector Selector selector)
    {
        this.serviceLocator = serviceLocator;
        this.selector = selector;
    }

    @Override
    public ServiceDescriptor getResponsibleService(String index)
    {
        try {
            return serviceLocator.selectServiceAtRandom(selector);
        }
        catch (ServiceNotAvailableException e) {
            return null;
        }
    }
}
