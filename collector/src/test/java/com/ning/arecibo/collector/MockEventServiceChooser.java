package com.ning.arecibo.collector;

import com.ning.arecibo.event.publisher.EventServiceChooser;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.util.service.ServiceDescriptor;

import java.io.IOException;
import java.util.UUID;

public class MockEventServiceChooser implements EventServiceChooser
{
    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    @Override
    public EventService choose(UUID uuid) throws IOException
    {
        return null;
    }

    @Override
    public void invalidate(UUID uuid)
    {
    }

    @Override
    public void onRemove(ServiceDescriptor sd)
    {
    }

    @Override
    public void onAdd(ServiceDescriptor sd)
    {
    }
}
