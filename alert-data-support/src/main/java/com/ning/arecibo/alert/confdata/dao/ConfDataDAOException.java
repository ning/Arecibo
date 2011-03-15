package com.ning.arecibo.alert.confdata.dao;

public class ConfDataDAOException extends Exception
{
    public ConfDataDAOException(String msg) {
        super(msg);
    }
    
    public ConfDataDAOException(String msg,Throwable t) {
        super(msg,t);
    }
}
