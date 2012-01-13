package com.ning.arecibo.event.receiver;

import org.skife.config.Config;
import org.skife.config.Default;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public abstract class UDPEventReceiverConfig
{
    int port = -1;

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
            if (port != -1) {
                return port;
            }

            try {
                ServerSocket sock = new ServerSocket();
                sock.bind(new InetSocketAddress(0));
                int value = sock.getLocalPort();
                sock.close();
                port = value;
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
