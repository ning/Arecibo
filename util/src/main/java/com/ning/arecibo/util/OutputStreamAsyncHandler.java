/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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