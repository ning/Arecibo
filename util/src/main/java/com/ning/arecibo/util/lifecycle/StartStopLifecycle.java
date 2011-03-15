package com.ning.arecibo.util.lifecycle;

import com.google.inject.Singleton;

/**
 * Provides a decent means of providing lifecycle events in the Guice container. To use this,
 * add a Lifecycle instance to Guice, possibly by building the injector with the LifecycleModule
 * in it.
 * <p/>
 * Once the Lifecycle is available as a component, make functions dependent on lifecycle events
 * depenent on the Lifecycle, and have them register their lifecycle callbacks.
 * <p/>
 * After the injector has been made, you can pull the lifecycle out and start it. Typical usage
 * should look like:
 * <code>
 * final Injector guice = Guice.createInjector(new LifecycleModule(),
 * new BarModule(),
 * new FooModule());
 * <p/>
 * final Lifecycle l = guice.getInstance(Lifecycle.class);
 * <p/>
 * l.start();
 * l.join();
 * </code>
 * <p/>
 * The {@link Lifecycle#join()}  call, at the end, will register a shutdown hook than join against the current thread.
 */
@Singleton
public class StartStopLifecycle extends AbstractLifecycle implements Lifecycle
{
    public StartStopLifecycle()
    {
        super(new RelaxedSequentialLifecycler(LifecycleEvent.START, LifecycleEvent.STOP).build(), true);
    }

    /**
     * Fire a Start event
     */
    public void start()
    {
        fire(LifecycleEvent.START);
    }

    /**
     * Fire a Stop event
     */
    public void stop()
    {
        fire(LifecycleEvent.STOP);
    }

    /**
     * Register a shutdown hook to fire a Stop event on JVM shutdown, and
     * join against the current thread
     *
     * @throws InterruptedException if the Thread.currentThread.join() is interrupted
     */
    public void join() throws InterruptedException
    {
        super.join(LifecycleEvent.STOP, true);
    }
}
