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
