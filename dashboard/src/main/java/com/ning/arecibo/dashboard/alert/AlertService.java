package com.ning.arecibo.dashboard.alert;

import com.ning.arecibo.util.service.ServiceDescriptor;

public class AlertService
{
    //TODO: Should inject this?
	public final static String NAME = "AreciboAlertService";

	public final static String HOST_KEY = "host";
	public final static String PORT_KEY = "port";
	public static final String SSL_PORT_KEY = "ssl-port";

	public final static class Selector implements com.ning.arecibo.util.service.Selector
	{
		public boolean match(ServiceDescriptor sd)
		{
			return sd.getName().equals(AlertService.NAME);
		}
	}
}
