package com.ning.arecibo.agent.datasource;

public class DataSourceException extends Exception {
	
	public DataSourceException(String message) {
		super(message);
	}
	
	public DataSourceException(String message,Throwable t) {
		super(message,t);
	}
}
