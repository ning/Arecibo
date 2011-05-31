package com.ning.arecibo.util.timeline;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ning.arecibo.util.Logger;

/**
 * This class represents a sequence of values for a single attribute,
 * e.g., "TP99 Response Time", for one host and one specific time range,
 * as the object is being accumulated.  It is not used to represent
 * past timeline sequences; they are held in TimelineChunk objects.
 * <p>
 * It accumulates samples in a byte array object
 */
public class SampleTimelineChunk {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final int DEFAULT_CHUNK_BYTE_ARRAY_SIZE = 300;

    // TODO: the HostTimeLines member isn't referenced here.
    // Figure out if we really need the backpointer.
    private final SampleSetTimelineChunk timelines;
    private final String sampleKind;
    private final ByteArrayOutputStream byteStream;
    /**
     * Any operation that writes to the outputStream must synchronize on it!
     */
    private final DataOutputStream outputStream;

    private int sampleCount;
    private SampleBase lastSample;

    public SampleTimelineChunk(SampleSetTimelineChunk timelines, String sampleKind) {
        this.timelines = timelines;
        this.sampleKind = sampleKind;
        this.byteStream = new ByteArrayOutputStream(DEFAULT_CHUNK_BYTE_ARRAY_SIZE);
        this.outputStream = new DataOutputStream(byteStream);
        lastSample = null;
        this.sampleCount = 0;
    }

    @SuppressWarnings("unchecked")
    public synchronized void addPlaceholder(final byte repeatCount) {
        if (repeatCount > 0) {
            addLastSample();
            lastSample = new RepeatedSample(repeatCount, new NullSample());
            sampleCount += repeatCount;
        }
    }

    /**
     * The log scanner can safely call this method, and know that the byte
     * array will always end in a complete sample
     * @return an instalce containing the bytes and the counts of samples
     */
    public synchronized EncodedBytesAndSampleCount getEncodedSamples() {
        if (lastSample != null) {
            SampleCoder.encodeSample(outputStream, lastSample);
        }
        try {
            outputStream.flush();
            return new EncodedBytesAndSampleCount(byteStream.toByteArray(), sampleCount);
        }
        catch (IOException e) {
            log.error(e, "In getEncodedSamples, IOException flushing outputStream");
            // Do no harm - - this at least won't corrupt the encoding
            return new EncodedBytesAndSampleCount(new byte[0], 0);
        }
    }

    private synchronized void addLastSample() {
        if (lastSample != null) {
            SampleCoder.encodeSample(outputStream, lastSample);
            lastSample = null;
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void addSample(final ScalarSample sample) {
        if (lastSample == null) {
            lastSample = sample;
        }
        else {
            final SampleOpcode lastOpcode = lastSample.getOpcode();
            final SampleOpcode sampleOpcode = sample.getOpcode();
            if (lastOpcode == SampleOpcode.REPEAT) {
                final RepeatedSample r = (RepeatedSample)lastSample;
                if (r.getSample().getOpcode() == sampleOpcode &&
                        (sampleOpcode == SampleOpcode.NULL || r.equals(sample)) &&
                        r.getRepeatCount() < RepeatedSample.MAX_REPEAT_COUNT) {
                    // We can just increment the count in the repeat instance
                    r.incrementRepeatCount();
                }
                else {
                    // A non-matching repeat - - just add it
                    addLastSample();
                    lastSample = sample;
                }
            }
            else {
                final ScalarSample lastScalarSample = (ScalarSample)lastSample;
                if (sampleOpcode == lastOpcode &&
                    ((sampleOpcode == SampleOpcode.NULL) || sample.getSampleValue().equals(lastScalarSample.getSampleValue()))) {
                    // Replace lastSample with repeat group
                    lastSample = new RepeatedSample((byte)2, lastScalarSample);
                }
                else {
                    addLastSample();
                    lastSample = sample;
                }
            }
        }
        // In all cases, we got 1 more sample
        sampleCount++;
    }

    public SampleSetTimelineChunk getTimelines() {
        return timelines;
    }

    public String getSampleKind() {
        return sampleKind;
    }

    public synchronized int getSampleCount() {
        return sampleCount;
    }
}
