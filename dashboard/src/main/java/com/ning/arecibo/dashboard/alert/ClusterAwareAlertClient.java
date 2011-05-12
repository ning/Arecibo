package com.ning.arecibo.dashboard.alert;

import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.guice.AlertHostOverride;
import com.ning.arecibo.dashboard.guice.AlertPortOverride;

import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceNotAvailableException;
import com.ning.arecibo.util.service.ServiceSelector;

public class ClusterAwareAlertClient
{
	private final static Logger log = Logger.getLogger(ClusterAwareAlertClient.class);

	private final ServiceLocator serviceLocator;
	private final AlertRESTClient api;
	private final Selector selector;
	
	private final String alertHostOverride;
	private final int alertPortOverride;

	private volatile int maxRetries = 3;

	@Inject
	public ClusterAwareAlertClient(ServiceLocator serviceLocator, 
	                               AlertRESTClient api,
	                               @AlertHostOverride String alertHostOverride,
	                               @AlertPortOverride int alertPortOverride)
	{
		this.serviceLocator = serviceLocator;
		this.api = api;
		this.alertHostOverride = alertHostOverride;
		this.alertPortOverride = alertPortOverride;

		if(alertHostOverride == null || alertHostOverride.length() == 0) {
		    this.selector = new ServiceSelector(AlertService.NAME);
		    log.info("Using prepared selector for alertService");
		}
		else {
		    this.selector = null;
		    log.info("Using override host/port for alert service, at: " + alertHostOverride + ":" + alertPortOverride);
		}
	}

	public void setMaxRetries(int maxRetries)
	{
		this.maxRetries = maxRetries;
	}
	
	public List<DashboardAlertStatus> getAlertStatus(Long generationCount)
	{
		int tries = maxRetries;
		IOException exception = null;

		while (tries > 0) {

		    String host;
		    int port;
		    
		    if(selector != null) {
		        ServiceDescriptor descriptor;
				try {
					descriptor = serviceLocator.selectServiceAtRandom(selector);
				}
				catch (ServiceNotAvailableException e) {
					log.error(e, "No alert servers available");
					throw new RuntimeException(e);
				}

				host = descriptor.getProperties().get(AlertService.HOST_KEY);
				port = Integer.parseInt(descriptor.getProperties().get(AlertService.PORT_KEY));
		    }
		    else {
		        host = this.alertHostOverride;
		        port = this.alertPortOverride;
		    }

			try {
				return api.getAlertStatus(host, port, generationCount);
			}
			catch (IOException e) {
				//log.info(e, "Error talking to alert server %s:%d to get alert status", host, port);

				--tries;
				exception = e;
			}
		}

		throw new IllegalStateException("Cannot get alert status", exception);
	}
}
