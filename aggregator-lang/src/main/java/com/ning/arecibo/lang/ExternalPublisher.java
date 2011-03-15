package com.ning.arecibo.lang;

import java.io.Serializable;

public class ExternalPublisher implements AggregationOutputProcessor, Serializable
{
	static final long serialVersionUID = 8102784457117462234L;

	private final boolean reliable ;
	private final String serviceName;

	public ExternalPublisher(String serviceName)
	{
		this(serviceName, false);
	}

	public ExternalPublisher(String serviceName, boolean reliable)
	{
		this.serviceName = serviceName;
		this.reliable = reliable;
	}

    public String getServiceName()
	{
		return serviceName;
	}

    public boolean isReliable() {
        return reliable;
    }
}
