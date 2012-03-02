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

/**
 * Interface to define the general contract between a ResponsibilityManager (like ConsistentHashingServiceChooser)
 * and other components.
 */
public interface ServiceChooser
{
    /**
     * Returns the service responsible for the specified index.
     *
     * @param index identifier representing an object in some partitioned space (for example, this could be the key to a distributed hash)
     * @return the ServiceDescriptor for the service in charge of the specified index.  Returns null if the responsibility manager is in an invalid state (not started) or if there is no service descriptor for the specified index
     */
    public ServiceDescriptor getResponsibleService(String index);
}
