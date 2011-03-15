package com.ning.arecibo.lang;

import com.ning.arecibo.eventlogger.Event;

public interface DispatchRouter
{
	String route(Event evt);
}
