package com.ning.arecibo.util.service;

/**
 * Interface to define the general contract between a ResponsibilityManager (like ConsistentHashingServiceChooser)
 * and other components.
 */
public interface ServiceChooser
{
    /**
     * Returns the service responsible for the specified index.
     *
     * @param index identifier representing an object in some partitioned space (for example, this could be the key to a distributed hash)
     * @return the ServiceDescriptor for the service in charge of the specified index.  Returns null if the responsibility manager is in an invalid state (not started) or if there is no service descriptor for the specified index
     */
    public ServiceDescriptor getResponsibleService(String index);
}
