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

import org.testng.annotations.Test;
import com.ning.arecibo.eventlogger.Event;
import static org.testng.Assert.assertEquals;

public class TestEvent
{
	class TestEventImpl extends Event
	{
		public TestEventImpl(long timestamp, String eventType, UUID sourceUUID)
		{
			super(timestamp, eventType, sourceUUID);
		}
	}

	@Test(groups = "fast")
	public void testConstructor()
	{
		Event e = new TestEventImpl(23, "foo", new UUID(1, 2));
		assertEquals(23, e.getTimestamp());
		assertEquals("foo", e.getEventType());
		assertEquals(new UUID(1, 2), e.getSourceUUID());
	}

	@Test(groups = "fast", expectedExceptions = IllegalArgumentException.class)
	public void testConstructor2() throws IllegalArgumentException
	{
		Event e = new TestEventImpl(23, null, new UUID(1, 2));
		e = new TestEventImpl(23, "foo", null);
	}

}
