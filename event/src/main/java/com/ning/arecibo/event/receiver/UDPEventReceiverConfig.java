package com.ning.arecibo.event.receiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.skife.config.Config;
import org.skife.config.Default;

public abstract class UDPEventReceiverConfig
{
    @Config("arecibo.host")
    @Default("0.0.0.0")
    public abstract String getHost();

    @Config("arecibo.udp.port")
    @Default("auto")
    public abstract String getPortValue();

    @Config("arecibo.udp.numThreads")
    @Default("50")
    public abstract int getNumUDPThreads();

    public int getPort()
    {
        if ("auto".equals(getPortValue())) {
            try {
                ServerSocket sock = new ServerSocket();
                sock.bind(new InetSocketAddress(0));
                int value = sock.getLocalPort();
                sock.close();
                return value;
            }
            catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        else {
            return Integer.valueOf(getPortValue());
        }
    }
}
