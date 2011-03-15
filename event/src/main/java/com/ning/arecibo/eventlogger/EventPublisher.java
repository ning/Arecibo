package com.ning.arecibo.eventlogger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A facility for logging {@link Event}s for analysis.
 */
public interface EventPublisher {
	
	/**
	 * Describes how events of this type should be published.
	 */
	public enum PublishMode {
		/**
		 * Events are published asynchronously. It is possible that events will be lost.
		 */
		ASYNCHRONOUS,
		
		/**
		 * Events are published synchronously to the local node. It is guaranteed that events will be logged
		 * to local stable storage before the publish call returns.
		 */
		SYNCHRONOUS_LOCAL,

		/**
		 * Events are published synchronously to the cluster. It is guaranteed that events will be logged
		 * to the cluster's stable storage before the publish call returns.
		 */
		SYNCHRONOUS_CLUSTER

	}

	/**
	 * Start the publisher. This must be called before {@link #publish(Event)} is called for the first time.
	 */
	void start() throws IOException;

	/**
	 * Publish an event asynchronously (using {@link PublishMode.ASYNCHRONOUS}).
	 * @param event the event to be published.
	 * @throws IOException if there is a problem publishing. In this case the event may or may not have been published.
	 * Applications that need events to be published exactly once should use a unique identifier in the event to detect this case.
	 */
    void publish(Event event) throws IOException;
    
	/**
	 * Publish an event using the given publish mode.
	 * @param event the event to be published.
	 * @param publishMode the publish mode
	 * @throws IOException if there is a problem publishing. In this case the event may or may not have been published.
	 * Applications that need events to be published exactly once should use a unique identifier in the event to detect this case.
	 */
    void publish(Event event, PublishMode publishMode) throws IOException;
        
    /**
     * Stop the publisher. The timeout is to specify how long to allow cleanup.
     * After the publisher has been stopped, no more methods may be called on it.
     * @param timeout
     * @param unit
     * @throws IOException
     * @throws InterruptedException
     */
    void stop(long timeout, TimeUnit unit) throws IOException, InterruptedException;

}
