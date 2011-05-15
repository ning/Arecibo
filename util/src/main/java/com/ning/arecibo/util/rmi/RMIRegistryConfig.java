package com.ning.arecibo.util.rmi;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.skife.config.Config;
import org.skife.config.Default;

public abstract class RMIRegistryConfig
{
    @Config("arecibo.rmi.port")
    @Default("auto")
    public abstract String getPortValue();

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
