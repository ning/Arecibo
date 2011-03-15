package com.ning.arecibo.lang;

import java.io.Serializable;

/**
 * A simple receiver specifies a host and port combo as the destination. The destination shall
 * be a event client accepting inbound UDP or REST requests.
 */
public class SimpleReceiver implements AggregationOutputProcessor, Serializable
{
	static final long serialVersionUID = -1812987318360951954L;

	private final String host ;
	private final int port ;
	private boolean reliable ;

	public SimpleReceiver(String host, int port, boolean reliable)
	{
		this.host = host;
		this.port = port;
		this.reliable = reliable;
	}

	public SimpleReceiver(String host, int port)
	{
		this.host = host;
		this.port = port;
		this.reliable = false;
	}

	public String getHost()
	{
		return host;
	}

	public int getPort()
	{
		return port;
	}

	public SimpleReceiver reliable()
	{
		reliable = true ;
		return this;
	}

	public SimpleReceiver unreliable()
	{
		reliable = false ;
		return this;
	}

	public boolean isReliable()
	{
		return reliable;
	}
}
