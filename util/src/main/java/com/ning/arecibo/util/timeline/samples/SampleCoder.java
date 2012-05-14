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

package com.ning.arecibo.util.timeline.samples;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import com.ning.arecibo.util.timeline.chunks.TimelineChunk;

public interface SampleCoder {

    public byte[] compressSamples(final List<ScalarSample> samples);

    public List<ScalarSample> decompressSamples(final byte[] sampleBytes) throws IOException;

    public void encodeSample(final DataOutputStream outputStream, final SampleBase sample);

    public void encodeScalarValue(final DataOutputStream outputStream, final SampleOpcode opcode, final Object value);

    public ScalarSample compressSample(final ScalarSample sample);

    public Object decodeScalarValue(final DataInputStream inputStream, final SampleOpcode opcode) throws IOException;

    public double getMaxFractionError();

    public byte[] combineSampleBytes(final List<byte[]> sampleBytesList);

    public void scan(final TimelineChunk chunk, final SampleProcessor processor) throws IOException;

    public void scan(final byte[] samples, final byte[] times, final int sampleCount, final SampleProcessor processor) throws IOException;
}
