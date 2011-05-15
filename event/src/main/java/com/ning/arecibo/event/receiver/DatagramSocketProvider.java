package com.ning.arecibo.event.receiver;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class DatagramSocketProvider implements Provider<DatagramSocket>
{
    private final UDPEventReceiverConfig config;

    @Inject
    public DatagramSocketProvider(UDPEventReceiverConfig config)
    {
        this.config = config;
    }

    public DatagramSocket get()
    {
        try {
            DatagramSocket socket = new DatagramSocket(new InetSocketAddress(config.getHost(), config.getPort()));
            //socket.bind();
            return socket ;
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}