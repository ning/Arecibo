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
