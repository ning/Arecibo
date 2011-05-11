package com.ning.arecibo.util.timeline;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.ning.arecibo.util.Logger;



public enum SampleOpcode {
    BYTE((byte)1, 1, false),
    SHORT((byte)2, 2, false),
    INT((byte)3, 4, false),
    LONG((byte)4, 8, false),
    FLOAT((byte)5, 4, false),
    DOUBLE((byte)6, 8, false),
    STRING((byte)7, 0, false),
    NULL((byte)8, 0, false),
    REPEAT((byte)9, 0, true);

    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();

    private byte opcodeIndex;
    private final int byteSize;
    private final boolean repeater;

    private SampleOpcode(byte opcodeIndex, int byteSize, boolean repeater) {
        this.opcodeIndex = opcodeIndex;
        this.byteSize = byteSize;
        this.repeater = repeater;
    }

    public byte getOpcodeIndex() {
        return opcodeIndex;
    }

    public static SampleOpcode getOpcodeFromIndex(final byte index) {
        for (SampleOpcode opcode : values()) {
            if (opcode.getOpcodeIndex() == index) {
                return opcode;
            }
        }
        final String s = String.format("In SampleOpcode.getOpcodefromIndex(), could not find opcode for index %d", index);
        log.error(s);
        throw new IllegalArgumentException(s);
    }

    /**
     * Output the scalar value into the output stream
     * @param outputStream the stream to which bytes should be written.
     * @param sample the sample value, interpreted according to the opcode.
     */
    public void encodeScalarSample(final DataOutputStream outputStream, final Object sample) throws IOException {
        switch (this) {
        case BYTE:
            outputStream.write((Byte)sample);
            break;
        case SHORT:
            outputStream.writeShort((Short)sample);
            break;
        case INT:
            outputStream.writeInt((Integer)sample);
            break;
        case LONG:
            outputStream.writeLong((Long)sample);
            break;
        case FLOAT:
            outputStream.writeFloat((Float)sample);
            break;
        case DOUBLE:
            outputStream.writeDouble((Double)sample);
            break;
        case STRING:
            final String s = (String)sample;
            final byte[] bytes = s.getBytes("UTF-8");
            outputStream.writeShort(s.length());
            outputStream.write(bytes, 0, bytes.length);
        default:
            final String err = String.format("In encodeScalarSample, opcode %s is unrecognized", this.name());
            log.error(err);
            throw new IllegalArgumentException(err);
        }
    }

    public Object decodeScalarSample(final DataInputStream inputStream) throws IOException {
        switch (this) {
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
            final String err = String.format("In decodeScalarSample, opcode %s unrecognized", this.name());
            log.error(err);
            throw new IllegalArgumentException(err);
        }
    }


    public int getByteSize() {
        return byteSize;
    }

    public boolean getRepeater() {
        return repeater;
    }
}
