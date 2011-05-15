package com.ning.arecibo.event.publisher;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.google.inject.Inject;
import com.ning.arecibo.eventlogger.AbstractEventPublisher;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceLocator;

public class AreciboEventPublisher extends AbstractEventPublisher
{
	private static final Logger log = Logger.getLogger(AreciboEventPublisher.class);
	private final EventServiceChooser chooser;
	private final ServiceLocator serviceLocator;
	private final Selector selector;
    private final ExecutorService globalExecutor;
    private final AsynchronousSender asyncSender;
	private final Spool spool;
	private final AtomicLong eventsDiscarded = new AtomicLong(0);
	private final AtomicLong restEventsDelivered = new AtomicLong(0);
	private final AtomicLong udpEventsDelivered = new AtomicLong(0);

    @Inject
	public AreciboEventPublisher(EventPublisherConfig config,
	                             EventServiceChooser chooser,
	                             ServiceLocator serviceLocator,
                                 @PublisherSelector Selector selector,
	                             @PublisherExecutor ExecutorService globalExecutor)
	{
		this.chooser = chooser;
		this.serviceLocator = serviceLocator;
		this.selector = selector;
        this.globalExecutor = globalExecutor;
        this.asyncSender = new AsynchronousSender(config.getMaxEventDispatchers(),
                                                  config.getMaxEventBufferSize(),
                                                  config.getMaxDrainDelay()) {
			protected void eventDiscarded(Runnable runnable)
			{
				eventsDiscarded.incrementAndGet();
				log.info("event %s discarded, total discarded = %d", runnable, eventsDiscarded.get());
			}
		};

		File spoolRoot = config.getLocalSpoolRoot().getAbsoluteFile();
		File publisherRoot = new File(spoolRoot, "publisher");
		this.spool = new Spool(config.getEventServiceName(),
		                       publisherRoot,
		                       this.serviceLocator,
		                       this.selector,
		                       this.globalExecutor,
		                       config.getSpooledEventExpiration()) {
			protected void sendViaREST(Event evt) throws IOException
			{
				sendREST(evt);
			}
		};
	}

	/**
	 * publish the event to EAS or logger.
	 *
	 * @param event
	 * @param publishMode
	 * @throws IOException
	 */
	public void publish(final Event event, PublishMode publishMode) throws IOException
	{
		switch (publishMode) {
			case ASYNCHRONOUS:
				if ( spool.hasCluster() ) {
					asyncSender.execute(new Runnable(){
						public void run()
						{
							try {
								chooser.choose(event.getSourceUUID()).sendUDP(event);
								udpEventsDelivered.getAndIncrement();
							}
							catch (IOException e) {
								log.error(e);
							}
						}
						public String toString()
						{
							return event.toString();
						}
					});
				}
				else {
					log.info("no AreciboEvent destination cluster available, discarding event ...");
					eventsDiscarded.incrementAndGet();
				}
				break;
			case SYNCHRONOUS_CLUSTER:
			case SYNCHRONOUS_LOCAL:
				if ( spool.hasCluster() ) {
					try {
						sendREST(event);
					}
					catch ( IOException e ) {
						log.error(e, "attempt to send sync event failed");
						spool.retry(event);
					}
				}
				else {
					log.debug("no AreciboEvent destination cluster available, spooling to disk ...");
					spool.spool(event);
				}
				break;
			default:
				break;
		}
	}

	private void sendREST(Event event) throws IOException
	{
		chooser.choose(event.getSourceUUID()).sendREST(event);
		restEventsDelivered.incrementAndGet();
	}

	public void start() throws IOException
	{
		this.chooser.start();
		this.spool.start();
	}

	public void stop(long l, TimeUnit timeUnit) throws IOException, InterruptedException
	{
		this.asyncSender.shutdown();
		this.asyncSender.awaitTermination(l,timeUnit);
		this.chooser.stop();
		this.spool.stop(l, timeUnit);
	}

    @MonitorableManaged(monitored = true)
    public int getSpoolQueueSize()
    {
        return spool.getSpoolQueueSize();
    }

    @MonitorableManaged(monitored = true)
    public int getRetryQueueSize()
    {
        return spool.getRetryQueueSize();
    }

    @MonitorableManaged(monitored = true)
	public int getEventBackLog()
	{
		return asyncSender.getQueueSize() ;
	}

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
    public long getEventsDiscarded()
    {
        return eventsDiscarded.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE})
    public long getUdpEventsDelivered()
    {
        return udpEventsDelivered.get();
    }

    @MonitorableManaged(monitored = true, monitoringType = { MonitoringType.COUNTER, MonitoringType.RATE })
	public long getRestEventsDelivered()
	{
		return restEventsDelivered.get();
	}
}
