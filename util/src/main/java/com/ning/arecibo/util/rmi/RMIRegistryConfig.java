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
