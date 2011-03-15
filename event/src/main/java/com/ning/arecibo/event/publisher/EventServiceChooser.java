package com.ning.arecibo.event.publisher;

import java.io.IOException;
import java.util.UUID;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.service.ServiceListener;

public interface EventServiceChooser extends ServiceListener
{
	public void start();
	public void stop();
	public EventService choose(UUID uuid) throws IOException;
	public void invalidate(UUID uuid);	
}
