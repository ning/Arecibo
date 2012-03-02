/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
