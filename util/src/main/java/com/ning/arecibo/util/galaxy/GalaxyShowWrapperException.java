package com.ning.arecibo.util.galaxy;

import java.io.IOException;

public class GalaxyShowWrapperException extends IOException
{
    public GalaxyShowWrapperException(String msg) {
        super(msg);
    }
    
    public GalaxyShowWrapperException(String msg,Throwable t) {
        super(msg,t);
    }
}
