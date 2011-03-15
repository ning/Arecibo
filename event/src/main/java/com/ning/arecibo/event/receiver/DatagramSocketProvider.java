package com.ning.arecibo.event.receiver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class DatagramSocketProvider implements Provider<DatagramSocket>
{
    private final String host;
    private final int port;

    @Inject
    public DatagramSocketProvider(@Named("UDPServerHost") String host, @Named("UDPServerPort") int port)
    {
        this.host = host;
        this.port = port;
    }

    public DatagramSocket get()
    {
        try {
            DatagramSocket socket = new DatagramSocket(new InetSocketAddress(host, port));
            //socket.bind();
            return socket ;
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
}