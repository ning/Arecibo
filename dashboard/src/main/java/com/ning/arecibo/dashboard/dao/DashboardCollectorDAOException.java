package com.ning.arecibo.dashboard.dao;

public class DashboardCollectorDAOException extends Exception
{
    public DashboardCollectorDAOException(String msg) {
        super(msg);
    }
    
    public DashboardCollectorDAOException(String msg,Throwable t) {
        super(msg,t);
    }
}
