package com.ning.arecibo.util.timeline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a sequence of values for a single attribute,
 * e.g., "TP99 Response Time", for one host and one specific time range
 */
public class SampleTimelineChunk {

    // TODO: the HostTimeLines member isn't referenced here.
    // Figure out if we really need the backpointer.
    private final SampleSetTimelineChunk timelines;
    private final String sampleKind;
    private final List<SampleBase> samples;

    private int sampleCount;

    public SampleTimelineChunk(SampleSetTimelineChunk timelines, String sampleKind) {
        this.timelines = timelines;
        this.sampleKind = sampleKind;
        this.samples = new ArrayList<SampleBase>();
        this.sampleCount = 0;
    }

    @SuppressWarnings("unchecked")
    public void addPlaceholder(final byte repeatCount) {
        if (repeatCount > 0) {
            samples.add(new RepeatedSample(repeatCount, new NullSample()));
            sampleCount += repeatCount;
        }
    }

    @SuppressWarnings("unchecked")
    public void addSample(final SampleBase sample) {
        final int size = samples.size();
        if (size == 0) {
            samples.add(sample);
        }
        else {
            // Get the previous sample
            final SampleBase previous = samples.get(size - 1);
            final SampleOpcode previousOpcode = previous.getOpcode();
            final SampleOpcode sampleOpcode = sample.getOpcode();
            RepeatedSample r;
            if (previousOpcode == SampleOpcode.REPEAT &&
                (r = (RepeatedSample)previous).getSample().getOpcode() == sampleOpcode &&
                (sampleOpcode == SampleOpcode.NULL || r.equals(sample)) &&
                r.getRepeatCount() < RepeatedSample.MAX_REPEAT_COUNT) {
                // We can just increment the count in the repeat instance
                r.incrementRepeatCount();
            }
            else if (sampleOpcode == previousOpcode && sample.equals(previous)) {
                // Replace the current ScalarSample at the end of the list with a REPEAT group.
                samples.set(size - 1, new RepeatedSample((byte)2, sample));
            }
            else {
                // None of the repeat optimizations work - - just add the item
                samples.add(sample);
            }
        }
        // In all cases, we got 1 more sample
        sampleCount++;
    }

    /**
     * This method converts the timeline into binary form, which
     * is the form saved in the db and scanned when read from the db.
     * @return a byte array containing the encoded representation of the time interval samples
     */
    @SuppressWarnings("unchecked")
    public byte[] encode() throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final DataOutputStream outputStream = new DataOutputStream(byteStream);
        for (SampleBase sample : samples) {
            // First put out the opcode value
            final SampleOpcode opcode = sample.getOpcode();
            RepeatedSample r;
            switch (opcode) {
            case REPEAT:
                outputStream.write(opcode.getOpcodeIndex());
                r = (RepeatedSample)sample;
                outputStream.write(r.getRepeatCount());
            case NULL:
                break;
            default:
                opcode.encodeScalarSample(outputStream, (ScalarSample)sample);
            }
        }
        outputStream.flush();
        return byteStream.toByteArray();
    }

    /**
     * This invoke the processor on the values in the timeline bytes.
     * @param bytes the byte representation of a timeline
     * @param processor the callback to which values value counts are passed to be processed.
     * @throws IOException
     */
    public static void scan(final byte[] bytes, final TimelineTimestamps timestamps, final SampleProcessor processor) throws IOException{
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        final DataInputStream inputStream = new DataInputStream(byteStream);
        int sampleCount = 0;
        while (true) {
            byte opcodeByte;
            try {
                opcodeByte = inputStream.readByte();
            }
            catch (EOFException e) {
                return;
            }
            final SampleOpcode opcode = SampleOpcode.getOpcodeFromIndex(opcodeByte);
            switch(opcode) {
            case REPEAT:
                final byte repeatCount = inputStream.readByte();
                final SampleOpcode repeatedOpcode = SampleOpcode.getOpcodeFromIndex(inputStream.readByte());
                final Object value = repeatedOpcode.decodeScalarSample(inputStream);
                processor.processSamples(timestamps, sampleCount, repeatCount, opcode, value);
                sampleCount += repeatCount;
                break;
            default:
                processor.processSamples(timestamps, sampleCount, 1, opcode, opcode.decodeScalarSample(inputStream));
                sampleCount += 1;
                break;
            }
        }
    }



    public SampleSetTimelineChunk getTimelines() {
        return timelines;
    }

    public String getSampleKind() {
        return sampleKind;
    }

    public int getSampleCount() {
        return sampleCount;
    }
}
