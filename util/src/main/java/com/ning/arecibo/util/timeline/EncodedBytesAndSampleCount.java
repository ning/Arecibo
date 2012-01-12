package com.ning.arecibo.util.timeline;

public class EncodedBytesAndSampleCount {
    private final byte[] encodedBytes;
    private final int sampleCount;

    public EncodedBytesAndSampleCount(byte[] encodedBytes, int sampleCount) {
        this.encodedBytes = encodedBytes;
        this.sampleCount = sampleCount;
    }

    public byte[] getEncodedBytes() {
        return encodedBytes;
    }

    public int getSampleCount() {
        return sampleCount;
    }
}
