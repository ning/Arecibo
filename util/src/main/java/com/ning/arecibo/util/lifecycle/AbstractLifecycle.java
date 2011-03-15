package com.ning.arecibo.util.lifecycle;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ning.arecibo.util.Logger;

/**
 * Base class for all kinds of life cycles that are in use. This contains the basic
 * plumbing needed everywhere.
 */
public abstract class AbstractLifecycle
{
    private static final Logger log = Logger.getLoggerViaExpensiveMagic();

    private final ConcurrentMap<LifecycleEvent, List<LifecycleListener>> listeners = new ConcurrentHashMap<LifecycleEvent, List<LifecycleListener>>();

    private final Lifecycler lifecycler;

    private final boolean verbose;

    /**
     * Build a new Lifecycle base.
     * @param lifecycler The lifecycler to provide the available events and the sequence in which they should be processed.
     * @param verbose If true, then report each stage at info logging level.
     */
    protected AbstractLifecycle(final Lifecycler lifecycler, boolean verbose)
    {
        this.verbose = verbose;
        this.lifecycler = lifecycler;

        // Add all events for that Lifecycle to the listener map.
        for (LifecycleEvent lifecycleEvent : lifecycler.getEvents()) {
            listeners.put(lifecycleEvent, new CopyOnWriteArrayList<LifecycleListener>());
            addListener(lifecycleEvent, lifecycler);
        }
    }

    /**
     * Adds a listener to a lifecycle event.
     *
     * @param event    The Lifecycle event on which to be notified, such as
     *                 {@link ning.guice.lifecycle.LifecycleEvent.Start} or {@link ning.guice.lifecycle.LifecycleEvent.Stop}
     * @param listener Callback to be invoked when the lifecycle event, <code>event</code>, is fired
     */
    public void addListener(final LifecycleEvent event, final LifecycleListener listener)
    {
        if (!listeners.containsKey(event)) {
            throw illegalEvent(event);
        }
        listeners.get(event).add(listener);
    }

    /**
     * @see Lifecycler#reset()
     */
    public void reset() {
        this.lifecycler.reset();
    }

    /**
     * @see Lifecycler#getNextEvent()
     */
    public LifecycleEvent getNextEvent() {
        return lifecycler.getNextEvent();
    }

    /**
     * Fire the next event in the cycle. By using this method, it is not necessary to
     * memorize which stage comes when.
     */
    public void fireNext() {
        LifecycleEvent nextEvent = lifecycler.getNextEvent();
        if (nextEvent == null) {
            throw new IllegalStateException("Lifecycle already hit the last event!");
        }
        fire(nextEvent);
    }

    /**
     * Fires the next event until the event requested has been reached.
     * @param lifecycleEvent The lifecycle event to reach. If the cycle ends before this event is reached, an IllegalStateException is thrown.
     * @param inclusive The requested event is also fired when true. Otherwise, the cycle is halted right before this event.
     */
    public void fireUpTo(final LifecycleEvent lifecycleEvent, final boolean inclusive) {

        boolean foundIt = false;
        do {
            final LifecycleEvent nextEvent = lifecycler.getNextEvent();

            if (nextEvent == null) {
                throw new IllegalStateException("Never reached event '" + lifecycleEvent.getName() + "' before ending the lifecycle.");
            }

            foundIt = nextEvent.equals(lifecycleEvent);
            if (!foundIt || inclusive) {
                fire(nextEvent);
            }
        } while(!foundIt);
    }

    /**
     * Fire a lifecycle event.
     * @param e The event to fire.
     */
    public void fire(final LifecycleEvent e)
    {
        log("Event '%s' starting", e.getName());

        final List<LifecycleListener> ls = listeners.get(e);
        if (ls == null) {
            throw illegalEvent(e);
        }
        for (LifecycleListener listener : ls) {
            listener.onEvent(e);
        }

        log("Event '%s' complete", e.getName());
    }

    /**
     * Register a shutdown hook to fire the given event on JVM shutdown, and
     * join against the current thread.
     *
     * @param e The event to reach.
     * @param cycle If true, then cycle to the event, otherwise, just fire the event.
     *
     * @throws InterruptedException if the Thread.currentThread.join() is interrupted
     */
    protected void join(final LifecycleEvent e, final boolean cycle) throws InterruptedException
    {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run()
            {
                if (cycle) {
                    AbstractLifecycle.this.fireUpTo(e, true);
                }
                else {
                    AbstractLifecycle.this.fire(e);
                }
            }
        });
        Thread.currentThread().join();
    }

    protected IllegalStateException illegalEvent(final LifecycleEvent event) {
        return new IllegalStateException(String.format("This lifecycle does not support the '%s' event, only '%s' are supported", event.getName(), lifecycler));
    }

    protected void log(final String message, Object ... args) {

        if (verbose) {
            log.info(message, args);
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug(message, args);
            }
        }
    }
}
