package com.ning.arecibo.event.receiver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.arecibo.util.Logger;

public class UDPServer
{
    private static final Logger log = Logger.getLogger(UDPServer.class);
    private final DatagramSocket socket;
    private final ExecutorService exe;
    private final UDPEventHandler handler;
    private final int port;
    private final byte[] buf;
    private volatile boolean isRunning = true;

    @Inject
    public UDPServer(@Named("UDPSocket") DatagramSocket socket, @Named("DatagramDispatcher") ExecutorService exe, UDPEventHandler handler, @Named("UDPServerPort") int port)
    {
        this.socket = socket;
        this.exe = exe;
        this.handler = handler;
        this.port = port;
        this.buf = new byte[65536];
    }

    public void start()
    {
        new Thread("UDPServer:recv"){

            @Override
            public void run()
            {
                log.info("Starting UDP Server on port %d", port);
                try
                {
                    while (isRunning) {
                        //TODO : use pooled buffer
                        final DatagramPacket p = new DatagramPacket(buf, buf.length);
                        socket.receive(p);

                        byte b[] = new byte[p.getLength()];
                        System.arraycopy(buf, 0, b, 0, b.length);

                        p.setData(b);

                        exe.execute(new Runnable(){
                            public void run()
                            {
                                try {
                                    handler.receive(p);
                                }
                                catch (Exception ex) {
                                    log.error(ex);
                                }
                            }
                        });
                    }
                }
                catch (IOException e){
                    if(isRunning)
                        log.error(e);
                }
            }
        }.start();
    }

    public synchronized void stop()
    {
        log.info("Stopping UDP Server on port %d", port);
        this.socket.close();
        this.isRunning = false;
        this.exe.shutdownNow();
    }
}
