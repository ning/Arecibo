package com.ning.arecibo.util.lifecycle;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Uses name to compare equality. Used with {@link ning.guice.lifecycle.Lifecycle#addListener(LifecycleEvent, LifecycleListener)}
 */
public final class LifecycleEvent
{
    /**
     * Lifecycle token used with {@link ning.guice.lifecycle.AbstractLifecycle#addListener(ning.guice.lifecycle.Lifecycle.LifecycleEvent,LifecycleListener)}
     */
    public static final LifecycleEvent START = new LifecycleEvent("Start");

    /**
     * Lifecycle token used with {@link ning.guice.lifecycle.AbstractLifecycle#addListener(ning.guice.lifecycle.Lifecycle.LifecycleEvent,LifecycleListener)}
     */
    public static final LifecycleEvent STOP = new LifecycleEvent("Stop");

    /**
     * Lifecycle token used with {@link ning.guice.lifecycle.AbstractLifecycle#addListener(ning.guice.lifecycle.Lifecycle.LifecycleEvent,LifecycleListener)}
     */
    public static final LifecycleEvent ANNOUNCE = new LifecycleEvent("Announce");

    /**
     * Lifecycle token used with {@link ning.guice.lifecycle.AbstractLifecycle#addListener(ning.guice.lifecycle.Lifecycle.LifecycleEvent,LifecycleListener)}
     */
    public static final LifecycleEvent CONFIGURE = new LifecycleEvent("Configure");

    /**
     * Lifecycle token used with {@link ning.guice.lifecycle.AbstractLifecycle#addListener(ning.guice.lifecycle.Lifecycle.LifecycleEvent,LifecycleListener)}
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
