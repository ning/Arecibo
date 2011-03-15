package com.ning.arecibo.aggregator.listeners;

import java.util.List;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.event.MapEvent;

public interface DefinedEventUpdateListener {
    public void update(final EventDefinition def, final List<MapEvent> events);
}
