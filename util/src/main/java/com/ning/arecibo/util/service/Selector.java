package com.ning.arecibo.util.service;

public interface Selector
{
    /**
     * Used to find services we are interested in
     */
    public boolean match(ServiceDescriptor sd);
}
