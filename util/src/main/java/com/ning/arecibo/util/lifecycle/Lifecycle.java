package com.ning.arecibo.util.lifecycle;



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
public interface Lifecycle
{
    /**
     * Adds a listener to a lifecycle event.
     *
     * @param event    The Lifecycle event on which to be notified, such as
     *                 {@link ning.guice.lifecycle.LifecycleEvent.Start} or {@link ning.guice.lifecycle.LifecycleEvent.Stop}
     * @param listener Callback to be invoked when the lifecycle event, <code>event</code>, is fired
     */
    void addListener(final LifecycleEvent event, final LifecycleListener listener);

    /**
     * @see Lifecycler#reset()
     */
    void reset();

    /**
     * @see Lifecycler#getNextEvent()
     */
    LifecycleEvent getNextEvent();

    /**
     * Fire the next event in the cycle. By using this method, it is not necessary to
     * memorize which stage comes when.
     */
    void fireNext();

    /**
     * Fires the next event until the event requested has been reached.
     * @param lifecycleEvent The lifecycle event to reach. If the cycle ends before this event is reached, an IllegalStateException is thrown.
     * @param inclusive The requested event is also fired when true. Otherwise, the cycle is halted right before this event.
     */
    void fireUpTo(final LifecycleEvent lifecycleEvent, final boolean inclusive);

    /**
     * Fire a lifecycle event.
     * @param e The event to fire.
     */
    void fire(final LifecycleEvent e);

    /**
     * Register a shutdown hook to fire a Stop event on JVM shutdown, and
     * join against the current thread
     *
     * @throws InterruptedException if the Thread.currentThread.join() is interrupted
     */
    void join() throws InterruptedException;
}
