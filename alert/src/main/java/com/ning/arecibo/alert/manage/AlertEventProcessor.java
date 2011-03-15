package com.ning.arecibo.alert.manage;

import java.util.concurrent.atomic.AtomicLong;
import com.google.inject.Inject;
import com.ning.arecibo.event.receiver.EventProcessor;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;

public class AlertEventProcessor implements EventProcessor
{
	private final AtomicLong eventsReceived = new AtomicLong(0L);
	private final AtomicLong eventsHandled = new AtomicLong(0L);
	
	private final AlertManager alertManager;
	private final AsynchronousEventHandler eventHandler;

	@Inject
	public AlertEventProcessor(AlertManager alertManager, AsynchronousEventHandler eventHandler)
	{
		this.alertManager = alertManager;
		this.eventHandler = eventHandler;
	}

	public void processEvent(Event evt)
	{
		//log.debug("Received Event : %s", evt);
		
		this.eventHandler.executeLater(new AlertEventRunnableHandler(evt));
		
		eventsReceived.getAndIncrement();
	}	

	@MonitorableManaged(monitored = true, monitoringType = MonitoringType.RATE)
	public long getEventsReceived()
	{
		return eventsReceived.get();
	}
	
	@MonitorableManaged(monitored = true, monitoringType = MonitoringType.RATE)
	public long getEventsHandled()
	{
		return eventsHandled.get();
	}
	
	public class AlertEventRunnableHandler implements Runnable {
		
		private final Event evt;
		
		public AlertEventRunnableHandler(Event evt) {
			this.evt = evt;
		}
		
		public Event getEvent() {
			return this.evt;
		}
		
		public void run() {
			String eventType = evt.getEventType();
			int endIndex = eventType.lastIndexOf('_');
			if(endIndex >= 0) {
	    		Long configId = Long.parseLong(eventType.substring(endIndex+1));
	    		alertManager.handleThresholdEvent(configId,evt);
	    		
	    		eventsHandled.getAndIncrement();
			}
		}	
	}
}
