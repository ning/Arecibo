package com.ning.arecibo.util.timeline;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import com.ning.arecibo.util.Logger;

public class SampleCoder {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    /**
     * This method writes the binary encoding of the sample to the outputStream.  This encoding
     * is the form saved in the db and scanned when read from the db.
     */
    @SuppressWarnings("unchecked")
    public static void encodeSample(final DataOutputStream outputStream, final SampleBase sample) {
        final SampleOpcode opcode = sample.getOpcode();
        try {
            // First put out the opcode value
            switch (opcode) {
            case REPEAT:
                final RepeatedSample r = (RepeatedSample)sample;
                final ScalarSample repeatee = r.getSample();
                outputStream.write(opcode.getOpcodeIndex());
                outputStream.write(r.getRepeatCount());
                encodeScalarValue(outputStream, repeatee.getOpcode(), repeatee.getSampleValue());
            case NULL:
                break;
            default:
                if (sample instanceof ScalarSample) {
                    encodeScalarValue(outputStream, opcode, ((ScalarSample)sample).getSampleValue());
                }
                else {
                    log.error("In encodeSample, opcode %s is not ScalarSample; instead ", opcode.name(), sample.getClass().getName());
                }
            }
        }
        catch (IOException e) {
            log.error(e, "In encodeScalarValue, IOException encoding opcode %s and value %s", opcode.name(), String.valueOf(sample));
        }
    }

    /**
     * Output the scalar value into the output stream
     * @param outputStream the stream to which bytes should be written.
     * @param value the sample value, interpreted according to the opcode.
     */
    public static void encodeScalarValue(final DataOutputStream outputStream, final SampleOpcode opcode, final Object value) {
        try {
            outputStream.write(opcode.getOpcodeIndex());
            switch (opcode) {
            case BYTE:
                outputStream.write((Byte)value);
                break;
            case SHORT:
                outputStream.writeShort((Short)value);
                break;
            case INT:
                outputStream.writeInt((Integer)value);
                break;
            case LONG:
                outputStream.writeLong((Long)value);
                break;
            case FLOAT:
                outputStream.writeFloat((Float)value);
                break;
            case DOUBLE:
                outputStream.writeDouble((Double)value);
                break;
            case STRING:
                final String s = (String)value;
                final byte[] bytes = s.getBytes("UTF-8");
                outputStream.writeShort(s.length());
                outputStream.write(bytes, 0, bytes.length);
            default:
                final String err = String.format("In encodeScalarSample, opcode %s is unrecognized", opcode.name());
                log.error(err);
                throw new IllegalArgumentException(err);
            }
        }
        catch (IOException e) {
            log.error(e, "In encodeScalarValue, IOException encoding opcode %s and value %s", opcode.name(), String.valueOf(value));
        }
    }

    public static Object decodeScalarValue(final DataInputStream inputStream, final SampleOpcode opcode) throws IOException {
        switch (opcode) {
        case NULL:
            return null;
        case BYTE:
            return new Byte(inputStream.readByte());
        case SHORT:
            return new Short(inputStream.readShort());
        case INT:
            return new Integer(inputStream.readInt());
        case LONG:
            return new Long(inputStream.readLong());
        case FLOAT:
            return new Float(inputStream.readFloat());
        case DOUBLE:
            return new Double(inputStream.readDouble());
        case STRING:
            final short s = inputStream.readShort();
            final byte[] bytes = new byte[s];
            final int byteCount = inputStream.read(bytes, 0, s);
            if (byteCount != s) {
                log.error("Reading string came up short");
            }
            return new String(bytes, "UTF-8");
        default:
            final String err = String.format("In decodeScalarSample, opcode %s unrecognized", opcode.name());
            log.error(err);
            throw new IllegalArgumentException(err);
        }
    }

    /**
     * This invokes the processor on the values in the timeline bytes.
     * @param bytes the byte representation of a timeline
     * @param processor the callback to which values value counts are passed to be processed.
     * @throws IOException
     */
    public static void scan(final byte[] bytes, final TimelineTimes timestamps, final SampleProcessor processor) throws IOException{
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
                final Object value = decodeScalarValue(inputStream, repeatedOpcode);
                processor.processSamples(timestamps, sampleCount, repeatCount, opcode, value);
                sampleCount += repeatCount;
                break;
            default:
                processor.processSamples(timestamps, sampleCount, 1, opcode, decodeScalarValue(inputStream, opcode));
                sampleCount += 1;
                break;
            }
        }
    }
}
