package com.ning.arecibo.lang;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;

public class ServiceSelector implements Selector, Serializable
{
	static final long serialVersionUID = 6476522335558352705L;

	private final String serviceName ;

	public ServiceSelector(String serviceName)
	{
		this.serviceName = serviceName;
	}

	public boolean match(ServiceDescriptor sd)
	{
		return StringUtils.equals(sd.getName(), serviceName);
	}
}
