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
