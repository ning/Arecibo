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

package com.ning.arecibo.collector;

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.TimeSpan;

public interface RealtimeClientConfig
{
    @Config("arecibo.rtClient.kafka.zkConnect")
    @Default("127.0.0.1:2181")
    String getZkConnect();

    @Config("arecibo.rtClient.kafka.zkConnectionTimeout")
    @Default("6s")
    TimeSpan getZkConnectionTimeout();

    @Config("arecibo.rtClient.kafka.groupId")
    @Default("arecibo")
    String getKafkaGroupId();

    @Config("arecibo.rtClient.kafka.nbThreads")
    @Default("1")
    int getNbThreads();
}