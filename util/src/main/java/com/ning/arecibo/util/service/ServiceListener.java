package com.ning.arecibo.util.service;

public interface ServiceListener
{
    public abstract void onRemove(ServiceDescriptor sd);
    public abstract void onAdd(ServiceDescriptor sd);
}
