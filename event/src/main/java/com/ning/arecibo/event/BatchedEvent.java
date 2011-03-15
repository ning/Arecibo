package com.ning.arecibo.event;


import java.util.ArrayList;
import java.util.List;
import com.ning.arecibo.eventlogger.Event;

public class BatchedEvent extends Event
{
	protected final List<Event> batch = new ArrayList<Event>();
	protected final Event first ;

	public BatchedEvent(Event firstEvt)
	{
		super(firstEvt.getTimestamp(), firstEvt.getEventType(), firstEvt.getSourceUUID());
		this.first = firstEvt ;
		batch.add(first);
	}

	public List<Event> getEvents()
	{
		return batch ;
	}


}
