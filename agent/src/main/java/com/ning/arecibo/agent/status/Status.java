package com.ning.arecibo.agent.status;

import com.ning.arecibo.agent.config.Config;

/**
 * Status
 * <p/>
 * <p/>
 * <p/>
 * Author: gary
 * Date: Jul 8, 2008
 * Time: 8:39:43 AM
 */
public final class Status
{
	private final Config config;
	private String valueName = null;

	// status information
	private long lastUpdateTime;
	private StatusType lastStatus;
	private String lastStatusMessage;
	private Object lastValue;

	public Status(Config config)
	{
		this.config = config;			// immutable, no need to clone or copy
		
		this.lastUpdateTime = 0;
		this.lastStatus = StatusType.UNINITIALIZED;
		this.lastStatusMessage = "";
	}

	public Status(Config config,String valueName)
	{
	    this(config);
		this.valueName = valueName;
	}
	
	///////////////////////////////////////////
	// for status
	//

	public StatusType getLastStatus()
	{
		return lastStatus;
	}

	public long getLastUpdateTime()
	{
		return lastUpdateTime;
	}

	public String getLastStatusMessage()
	{
		return lastStatusMessage;
	}

	public void setLastStatus(StatusType lastStatus, long updateTime, String message, Object value)
	{
		this.lastStatus = lastStatus;
		this.lastUpdateTime = updateTime;
		
		if(message == null)
		    message = "";
		
		this.lastStatusMessage = message;

		this.lastValue = value ;
	}


	public String getHost()
	{
		return config.getHost();
	}

	public String getEventName()
	{
		return config.getEventType();
	}

	public String getKeyName()
	{
	    return config.getEventAttributeType();
	}
	
	public String getValueName()
	{
	    return this.valueName;
	}
	
	public String getCompoundKeyName()
	{
	    if(valueName != null)
	        return config.getEventAttributeType() + ":" + valueName;
	    else
	        return config.getEventAttributeType();
	}
	
	public Object getLastValue()
	{
		return lastValue;
	}
}
