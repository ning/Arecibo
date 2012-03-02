package com.ning.arecibo.util.service;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

public class ServiceSelector implements Selector, Serializable
{
    static final long serialVersionUID = 6476522335558352705L;

    private final String serviceName;

    public ServiceSelector(String serviceName)
    {
        this.serviceName = serviceName;
    }

    public String getServiceName()
    {
        return serviceName;
    }

    public boolean match(ServiceDescriptor sd)
    {
        return StringUtils.equals(sd.getName(), serviceName);
    }
}
