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

import com.google.inject.Binder;
import com.google.inject.Module;

public class LifecycleModule implements Module
{
    private final Lifecycle lifecycle;

    /**
     * Creates a new Lifecycle Module with the expected (i.e. Start/Stop) behaviour.
     */
    public LifecycleModule()
    {
        this(new StartStopLifecycle());
    }

    /**
     * Creates a new Lifecycle Module with whatever Lifecycle you want to use.
     */
    public LifecycleModule(final Lifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }

    /**
     * @see Module#configure(Binder)
     */
    public void configure(Binder binder)
    {
        binder.bind(Lifecycle.class).toInstance(lifecycle);
    }
}
