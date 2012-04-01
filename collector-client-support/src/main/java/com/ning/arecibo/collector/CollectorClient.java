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

import com.ning.arecibo.util.timeline.CategoryAndSampleKinds;
import com.ning.arecibo.util.timeline.SamplesForSampleKindAndHost;
import com.ning.arecibo.util.timeline.TimelineChunkAndTimes;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.joda.time.DateTime;

import java.io.InputStream;

public interface CollectorClient
{
    public InputStream getHostsAsStream() throws UniformInterfaceException;

    public Iterable<String> getHosts() throws UniformInterfaceException;

    public InputStream getSampleKindsAsStream() throws UniformInterfaceException;

    public InputStream getSampleKindsAsStream(final Iterable<String> hostNames) throws UniformInterfaceException;

    public Iterable<CategoryAndSampleKinds> getSampleKinds() throws UniformInterfaceException;

    public Iterable<CategoryAndSampleKinds> getSampleKinds(final Iterable<String> hostNames) throws UniformInterfaceException;

    public InputStream getHostSamplesAsStream(final Iterable<String> hostNames, final Iterable<String> categoriesAndSampleKinds, final DateTime from, final DateTime to) throws UniformInterfaceException;

    public Iterable<SamplesForSampleKindAndHost> getHostSamples(final Iterable<String> hostNames, final Iterable<String> categoriesAndSampleKinds, final DateTime from, final DateTime to) throws UniformInterfaceException;
}
