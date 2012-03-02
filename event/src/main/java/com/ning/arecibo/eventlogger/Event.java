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
