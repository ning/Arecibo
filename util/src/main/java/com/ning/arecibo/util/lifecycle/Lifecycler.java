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

package com.ning.arecibo.util.lifecycle;

import java.util.List;

/**
 * Defines an uniform interface for various lifecycle strategies. The most obvious is a sequential lifecycle
 * which starts at the beginning and the progresses through various stages to an end.
 */
public interface Lifecycler extends LifecycleListener
{
    /**
     * Returns a list of all Lifecycle events in this Strategy.
     * @return List of Lifecycle Events. May be empty, is never null.
     */
    List<LifecycleEvent> getEvents();

    /**
     * Reset this Strategy to its starting point.
     */
    void reset();

    /**
     * Returns the Event that should be fired next. This method can be called
     * multiple times without changing the state of the Lifecycler.
     *
     * @return The next LifecycleEvent to fire or null if all Events have been fired.
     */
    LifecycleEvent getNextEvent();

    /**
     * @see LifecycleListener#onEvent(LifecycleEvent)
     */
    void onEvent(LifecycleEvent e);
}
