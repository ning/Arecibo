package com.ning.arecibo.aggregator.listeners;

import com.ning.arecibo.aggregator.dictionary.EventDefinition;

public interface EventRegistrationListener
{
	void eventRegistered(EventDefinition def);
    void eventUnRegistered(EventDefinition def);
}
