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

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Primary interface to find and offer services.
 */
public interface ServiceLocator
{
    /**
     * Start the service locator. This will start advertising local services, and make
     * available selecting remote services.
     */
    void start();

    /**
     * Start the service locator but not start advertising local services, and make
     * available selecting remote services.
     */
    void startReadOnly();

    /**
     * Stop the cluster. This will stop advertising local services.
     */
    void stop();

    /**
     * Advertise a local service.
     */
    public void advertiseLocalService(ServiceDescriptor sd);

    /**
     * Registers a listener that is called whenever a service matching the selector
     * is added ({@link ServiceListener#onAdd(ServiceDescriptor)}) or removed ({@link ServiceListener#onRemove(ServiceDescriptor)}).
     * <p/>
     * Note that registering a listener will cause its selector to be run on the
     * current set of services and then the differences (adds and removes) from
     * there after.
     *
     * @param selector if match(...) returns true, the listener will be called
     * @param executor the execution environment for the listener to be called in
     * @param listener defines callback behavior
     */
    void registerListener(Selector selector, Executor executor, ServiceListener listener);

    /**
     * Unregisters the given listener.
     *
     * @param listener The listener to remove
     */
    void unregisterListener(ServiceListener listener);

    /**
     * Execute a one-shot selector
     */
    Set<ServiceDescriptor> selectServices(Selector selector);

    /**
     * Used to select one of N service descripors at random
     *
     * @param key Named selector to use
     * @return one of N backends, selected at random
     * @throws ServiceNotAvailableException if no services are available for this selector
     */
    ServiceDescriptor selectServiceAtRandom(Selector key) throws ServiceNotAvailableException;
}
