package com.ning.arecibo.eventlogger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A base implementation of {@link EventPublisher}. Subclasses need only implement
 * {@link #publish(Event, PublishMode)}.
 * </p>
 */
public abstract class AbstractEventPublisher implements EventPublisher {
	public void publish(Event event) throws IOException {
		publish(event, PublishMode.ASYNCHRONOUS);
	}

	public void start() throws IOException {
		// do nothing
	}

	public void stop(long timeout, TimeUnit unit) throws IOException,
			InterruptedException {
		// do nothing
	}

}
