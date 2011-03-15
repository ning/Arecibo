package com.ning.arecibo.collector.dao;

public class CollectorDAOException extends Exception
{
    public CollectorDAOException(String msg) {
        super(msg);
    }
    
    public CollectorDAOException(String msg,Throwable t) {
        super(msg,t);
    }
}
