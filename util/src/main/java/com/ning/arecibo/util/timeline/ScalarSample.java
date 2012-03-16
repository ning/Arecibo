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

package com.ning.arecibo.util.timeline;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonValue;

import java.util.Map;

/**
 * @param <T> A value consistent with the opcode
 */
public class ScalarSample<T> extends SampleBase
{
    private static final String KEY_OPCODE = "O";
    private static final String KEY_SAMPLE_VALUE = "V";

    private final T sampleValue;

    public ScalarSample(final SampleOpcode opcode, final T sampleValue)
    {
        super(opcode);
        this.sampleValue = sampleValue;
    }

    public ScalarSample(final String opcode, final T sampleValue)
    {
        super(SampleOpcode.valueOf(opcode));
        this.sampleValue = sampleValue;
    }

    @JsonCreator
    public ScalarSample(@JsonProperty(KEY_OPCODE) final byte opcodeIdx, @JsonProperty(KEY_SAMPLE_VALUE) final T sampleValue)
    {
        super(SampleOpcode.getOpcodeFromIndex(opcodeIdx));
        this.sampleValue = sampleValue;
    }

    public T getSampleValue()
    {
        return sampleValue;
    }

    @Override
    public String toString()
    {
        return sampleValue.toString();
    }

    @JsonValue
    public Map<String, Object> toMap()
    {
        return ImmutableMap.of(KEY_OPCODE, opcode.getOpcodeIndex(), KEY_SAMPLE_VALUE, sampleValue);
    }
}
