package com.ning.arecibo.event.receiver;

import com.ning.arecibo.eventlogger.Event;

public interface EventProcessor extends BaseEventProcessor
{
	public void processEvent(Event evt);
}
