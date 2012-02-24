package com.ning.arecibo.util.timeline;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @param <T> A value consistent with the opcode
 */
public class ScalarSample<T> extends SampleBase
{

    private final T sampleValue;

    public ScalarSample(final SampleOpcode opcode, final T sampleValue)
    {
        super(opcode);
        this.sampleValue = sampleValue;
    }

    @JsonCreator
    public ScalarSample(@JsonProperty("opcode") final String opcode, @JsonProperty("sampleValue") final T sampleValue)
    {
        super(SampleOpcode.valueOf(opcode));
        this.sampleValue = sampleValue;
    }

    public T getSampleValue()
    {
        return sampleValue;
    }
}
