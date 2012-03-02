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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Uses name to compare equality. Used with {@link com.ning.arecibo.util.lifecycle.Lifecycle#addListener(LifecycleEvent, LifecycleListener)}
 */
public final class LifecycleEvent
{
    /**
     * Lifecycle token used with {@link com.ning.arecibo.util.lifecycle.AbstractLifecycle#addListener(com.ning.arecibo.util.lifecycle.LifecycleEvent, LifecycleListener)}
     */
    public static final LifecycleEvent START = new LifecycleEvent("Start");

    /**
     * Lifecycle token used with {@link com.ning.arecibo.util.lifecycle.AbstractLifecycle#addListener(com.ning.arecibo.util.lifecycle.LifecycleEvent, LifecycleListener)}
     */
    public static final LifecycleEvent STOP = new LifecycleEvent("Stop");

    /**
     * Lifecycle token used with {@link com.ning.arecibo.util.lifecycle.AbstractLifecycle#addListener(com.ning.arecibo.util.lifecycle.LifecycleEvent, LifecycleListener)}
     */
    public static final LifecycleEvent ANNOUNCE = new LifecycleEvent("Announce");

    /**
     * Lifecycle token used with {@link com.ning.arecibo.util.lifecycle.AbstractLifecycle#addListener(com.ning.arecibo.util.lifecycle.LifecycleEvent, LifecycleListener)}
     */
    public static final LifecycleEvent CONFIGURE = new LifecycleEvent("Configure");

    /**
     * Lifecycle token used with {@link com.ning.arecibo.util.lifecycle.AbstractLifecycle#addListener(com.ning.arecibo.util.lifecycle.LifecycleEvent, LifecycleListener)}
     */
    public static final LifecycleEvent UNANNOUNCE = new LifecycleEvent("Unannounce");

    private final String name;

    public LifecycleEvent(final String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("name", getName())
            .toString();
    }
}
