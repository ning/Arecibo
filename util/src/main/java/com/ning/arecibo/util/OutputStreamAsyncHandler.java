package com.ning.arecibo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

public class OutputStreamAsyncHandler implements AsyncHandler<Void>
{
    private final PipedInputStream  pipeIn;
    private final PipedOutputStream pipeOut;

    public OutputStreamAsyncHandler()
    {
        try {
            this.pipeIn = new PipedInputStream();
            this.pipeOut = new PipedOutputStream(pipeIn);
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public InputStream getInputStream()
    {
        return pipeIn;
    }
    
    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart part) throws Exception
    {
        part.writeTo(pipeOut);
        pipeOut.flush();
        return STATE.CONTINUE;
    }

    @Override
    public Void onCompleted() throws Exception
    {
        pipeOut.close();
        pipeIn.close();
        return null;
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception
    {
        return STATE.CONTINUE;
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus status) throws Exception
    {
        if (status.getStatusCode() == 200) {
            return STATE.CONTINUE;
        }
        else {
            return STATE.ABORT;
        }
    }

    @Override
    public void onThrowable(Throwable ex)
    {
    }
}