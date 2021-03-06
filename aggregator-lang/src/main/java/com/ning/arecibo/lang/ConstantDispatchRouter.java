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
import com.ning.arecibo.eventlogger.Event;

public class ConstantDispatchRouter implements DispatchRouter, Serializable
{
	static final long serialVersionUID = -959247313581275980L;

	private final String theConstant ;

	public ConstantDispatchRouter(String theConstant)
	{
		this.theConstant = theConstant;
	}

	public String getTheConstant()
	{
		return theConstant;
	}

	public String route(Event evt)
	{
		return theConstant;
	}
}
