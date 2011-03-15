package com.ning.arecibo.eventlogger;

import java.util.UUID;

/**
 * Subclasses of {@link Event} contain the fields to be published by an {@link EventPublisher}.
 */
public abstract class Event implements java.io.Serializable
{
	static final long serialVersionUID = 8038612113074182739L;

	private final long timestamp;
	private final String eventType;
	private final UUID sourceUUID;

	public Event(long timestamp, String eventType, UUID sourceUUID)
	{
		this.timestamp = timestamp;
		if ( eventType == null || sourceUUID == null )
		{
			throw new IllegalArgumentException("eventType and sourceUUID cannot be null");
		}
		this.eventType = eventType;
		this.sourceUUID = sourceUUID;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public String getEventType()
	{
		return eventType;
	}

	public UUID getSourceUUID()
	{
		return sourceUUID;
	}

}
