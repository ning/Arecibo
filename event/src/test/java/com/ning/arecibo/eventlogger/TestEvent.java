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
