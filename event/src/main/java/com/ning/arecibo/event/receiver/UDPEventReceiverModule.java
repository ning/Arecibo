package com.ning.arecibo.event.receiver;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.weakref.jmx.guice.ExportBuilder;
import org.weakref.jmx.guice.MBeanModule;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.ning.arecibo.util.lifecycle.LifecycleAction;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycledProvider;

public class UDPEventReceiverModule implements Module
{
    private final int numUDPThreads ;

    public UDPEventReceiverModule()
    {
        numUDPThreads = Integer.getInteger("arecibo.udp.numThreads", 50);
    }

    public UDPEventReceiverModule(int n)
    {
        numUDPThreads = Integer.getInteger("arecibo.udp.numThreads", n);
    }

    public void configure(Binder binder)
    {
        String host = System.getProperty("arecibo.host", "0.0.0.0");
        String portValue = System.getProperty("arecibo.udp.port", "auto");
        int port = "auto".equals(portValue) ? getRandomPort() : Integer.valueOf(portValue);

        binder.bindConstant().annotatedWith(Names.named("UDPServerHost")).to(host);
        binder.bindConstant().annotatedWith(Names.named("UDPServerPort")).to(port);
        binder.bind(DatagramSocket.class).annotatedWith(Names.named("UDPSocket")).toProvider(DatagramSocketProvider.class);
        binder.bind(ExecutorService.class).annotatedWith(Names.named("DatagramDispatcher")).toInstance(Executors.newFixedThreadPool(numUDPThreads));
        binder.bind(UDPEventHandler.class).asEagerSingleton();

        LifecycledProvider<UDPServer> lifecycledProvider = new LifecycledProvider<UDPServer>(binder, UDPServer.class);
        lifecycledProvider.addListener(LifecycleEvent.START, new LifecycleAction<UDPServer>()
        {
            public void doAction(UDPServer server)
            {
                try {
                    server.start();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        lifecycledProvider.addListener(LifecycleEvent.STOP, new LifecycleAction<UDPServer>()
        {
            public void doAction(UDPServer server)
            {
                server.stop();
            }
        });

        binder.bind(UDPServer.class).toProvider(lifecycledProvider).asEagerSingleton();

        ExportBuilder builder = MBeanModule.newExporter(binder);

        builder.export(UDPEventHandler.class).as("arecibo:name=UDPEventHandler");
    }

    private int getRandomPort()
    {
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
}
