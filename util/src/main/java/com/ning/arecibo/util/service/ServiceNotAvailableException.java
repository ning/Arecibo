package com.ning.arecibo.util.service;

public class ServiceNotAvailableException extends Exception
{
    public ServiceNotAvailableException()
    {
    }

    public ServiceNotAvailableException(String message)
    {
        super(message);
    }

    public ServiceNotAvailableException(Throwable cause)
    {
        super(cause);
    }

    public ServiceNotAvailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
