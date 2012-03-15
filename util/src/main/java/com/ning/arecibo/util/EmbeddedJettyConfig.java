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

import org.skife.config.Config;
import org.skife.config.Default;

public abstract class EmbeddedJettyConfig
{
    @Config("arecibo.host")
    @Default("0.0.0.0")
    public abstract String getHost();

    @Config("arecibo.jetty.port")
    @Default("8088")
    public abstract int getPort();

    @Config("arecibo.jetty.threads.min")
    @Default("1")
    public abstract int getMinThreads();

    @Config("arecibo.jetty.threads.max")
    @Default("200")
    public abstract int getMaxThreads();

    @Config("arecibo.jetty.accept-queue")
    @Default("200")
    public abstract int getAcceptQueueSize();

    @Config("arecibo.jetty.resourceBase")
    @Default("src/main/webapp")
    public abstract String getResourceBase();
}
