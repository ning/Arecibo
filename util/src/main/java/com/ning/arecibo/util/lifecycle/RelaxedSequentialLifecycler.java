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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Trivial implementation of a Lifecycle. Contains a starting and stopping point and events are fired
 * in Sequence. This Lifecycler allows events to be fired multiple times and does not enforce the sequence.
 */
public class RelaxedSequentialLifecycler implements Lifecycler, LifecycleListener
{
    /** List of all events in this cycle */
    private final LinkedList<LifecycleEvent> lifecycleEvents = new LinkedList<LifecycleEvent>();

    /** Map to find the next event */
    private final Map<LifecycleEvent, LifecycleEvent> eventMap = new HashMap<LifecycleEvent, LifecycleEvent>();

    /** The next event to fire */
    private LifecycleEvent nextEvent = null;

    private boolean built = false;

    /**
     * Builds a new Lifecycler. Events can be passed as C'tor arguments.
     * @param events Events to add to the Lifecycle.
     */
    public RelaxedSequentialLifecycler(final LifecycleEvent ...events) {
        if (events != null) {
            for (LifecycleEvent event : events) {
                add(event);
            }
        }
    }

    /**
     * Add a new Event to the end of the lifecycle. Can be called as long as the
     * Lifecycler has not yet been built.
     */
    public synchronized RelaxedSequentialLifecycler add(final LifecycleEvent event) {
        if (built) {
            throw new IllegalStateException("Lifecyler is already sealed!");
        }

        if (lifecycleEvents.size() > 0) {
            eventMap.put(lifecycleEvents.getLast(), event);
        }

        eventMap.put(event, null);
        lifecycleEvents.add(event);
        return this;
    }

    /**
     * Seal the Lifecycler up and make it ready for cycling.
     */
    public synchronized RelaxedSequentialLifecycler build() {
        if (built) {
            throw new IllegalStateException("Lifecyler is already sealed!");
        }

        built = true;
        reset();
        return this;
    }

    @Override
    public void reset() {
        if (!built) {
            throw new IllegalStateException("Lifecyler must be sealed!");
        }

        nextEvent = (lifecycleEvents.size() > 0) ? lifecycleEvents.getFirst() : null;
    }

    @Override
    public List<LifecycleEvent> getEvents() {
        if (!built) {
            throw new IllegalStateException("Lifecyler must be sealed!");
        }

        return Collections.unmodifiableList(lifecycleEvents);
    }

    @Override
    public LifecycleEvent getNextEvent() {
        if (!built) {
            throw new IllegalStateException("Lifecyler must be sealed!");
        }

        return nextEvent;
    }


    @Override
    public void onEvent(final LifecycleEvent e)
    {
        if (!built) {
            throw new IllegalStateException("Lifecyler must be sealed!");
        }

        if (eventMap.containsKey(e)) {
            nextEvent = eventMap.get(e);
        }
        else {
            throw new IllegalStateException("LifecycleEvent '" + e.getName() + "' was unexpected!");
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(this.getClass().getName()).append('[');
        for (Iterator<LifecycleEvent> it = lifecycleEvents.iterator(); it.hasNext(); ) {
            LifecycleEvent le = it.next();
            boolean ne = nextEvent != null && le.equals(nextEvent);
            if (ne) {
                sb.append("*");
            }

            sb.append(le.getName());

            if (ne) {
                sb.append("*");
            }

            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
}
