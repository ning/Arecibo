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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * @param <T> A value consistent with the opcode
 */
public class ScalarSample<T> extends SampleBase
{
    private static final String KEY_SAMPLE_VALUE = "V";

    @JsonProperty(KEY_SAMPLE_VALUE)
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T sampleValue;

    public static ScalarSample<?> fromObject(final Object sampleValue)
    {
        if (sampleValue == null) {
            return new ScalarSample<Void>(SampleOpcode.NULL, null);
        }
        else if (sampleValue instanceof Byte) {
            return new ScalarSample<Byte>(SampleOpcode.BYTE, (Byte) sampleValue);
        }
        else if (sampleValue instanceof Short) {
            return new ScalarSample<Short>(SampleOpcode.SHORT, (Short) sampleValue);
        }
        else if (sampleValue instanceof Integer) {
            try {
                // Can it fit in a short?
                final short optimizedShort = Shorts.checkedCast(Long.valueOf(sampleValue.toString()));
                return new ScalarSample<Short>(SampleOpcode.SHORT, optimizedShort);
            }
            catch (IllegalArgumentException e) {
                return new ScalarSample<Integer>(SampleOpcode.INT, (Integer) sampleValue);
            }
        }
        else if (sampleValue instanceof Long) {
            try {
                // Can it fit in a short?
                final short optimizedShort = Shorts.checkedCast(Long.valueOf(sampleValue.toString()));
                return new ScalarSample<Short>(SampleOpcode.SHORT, optimizedShort);
            }
            catch (IllegalArgumentException e) {
                try {
                    // Can it fit in an int?
                    final int optimizedLong = Ints.checkedCast(Long.valueOf(sampleValue.toString()));
                    return new ScalarSample<Integer>(SampleOpcode.INT, optimizedLong);
                }
                catch (IllegalArgumentException ohWell) {
                    return new ScalarSample<Long>(SampleOpcode.LONG, (Long) sampleValue);
                }
            }
        }
        else if (sampleValue instanceof Float) {
            return new ScalarSample<Float>(SampleOpcode.FLOAT, (Float) sampleValue);
        }
        else if (sampleValue instanceof Double) {
            return new ScalarSample<Double>(SampleOpcode.DOUBLE, (Double) sampleValue);
        }
        else {
            return new ScalarSample<String>(SampleOpcode.STRING, sampleValue.toString());
        }
    }

    @JsonCreator
    public ScalarSample(@JsonProperty(KEY_OPCODE) final SampleOpcode opcode, @JsonProperty(KEY_SAMPLE_VALUE) final T sampleValue)
    {
        super(opcode);
        this.sampleValue = sampleValue;
    }

    public ScalarSample(final String opcode, final T sampleValue)
    {
        super(SampleOpcode.valueOf(opcode));
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
}
