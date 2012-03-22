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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;

import com.ning.arecibo.util.Logger;

/**
 * This class contains a collection of static methods used to encode samples, and also
 * play back streams of samples
 */
public class SampleCoder {
    private static final Logger log = Logger.getCallersLoggerViaExpensiveMagic();
    private static final BigInteger BIGINTEGER_ZERO_VALUE = new BigInteger("0");
    private static final ScalarSample<Void> DOUBLE_ZERO_SAMPLE = new ScalarSample<Void>(SampleOpcode.DOUBLE_ZERO, null);
    private static final ScalarSample<Void> INT_ZERO_SAMPLE = new ScalarSample<Void>(SampleOpcode.INT_ZERO, null);

    // TODO: Figure out if 1/200 is an acceptable level of inaccuracy
    // For the HalfFloat, which has a 10-bit mantissa, this means that it could differ
    // in the last 3 bits of the mantissa and still be treated as matching.
    public static final double MAX_FRACTION_ERROR = 1.0 / 200.0;
    public static final double HALF_MAX_FRACTION_ERROR = MAX_FRACTION_ERROR / 2.0;

    private static final double MIN_BYTE_DOUBLE_VALUE = ((double)Byte.MIN_VALUE) * (1.0 + HALF_MAX_FRACTION_ERROR);
    private static final double MAX_BYTE_DOUBLE_VALUE = ((double)Byte.MAX_VALUE) * (1.0 + HALF_MAX_FRACTION_ERROR);

    private static final double MIN_SHORT_DOUBLE_VALUE = ((double)Short.MIN_VALUE) * (1.0 + HALF_MAX_FRACTION_ERROR);
    private static final double MAX_SHORT_DOUBLE_VALUE = ((double)Short.MAX_VALUE) * (1.0 + HALF_MAX_FRACTION_ERROR);

    @SuppressWarnings("unused")
    private static final double INVERSE_MAX_FRACTION_ERROR = 1.0 / MAX_FRACTION_ERROR;

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
                final RepeatSample r = (RepeatSample)sample;
                final ScalarSample repeatee = r.getSampleRepeated();
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
            case NULL:
            case DOUBLE_ZERO:
            case INT_ZERO:
                break;
            case BYTE:
            case BYTE_FOR_DOUBLE:
                outputStream.writeByte((Byte)value);
                break;
            case SHORT:
            case SHORT_FOR_DOUBLE:
            case HALF_FLOAT_FOR_DOUBLE:
                outputStream.writeShort((Short)value);
                break;
            case INT:
                outputStream.writeInt((Integer)value);
                break;
            case LONG:
                outputStream.writeLong((Long)value);
                break;
            case FLOAT:
            case FLOAT_FOR_DOUBLE:
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
                break;
            case BIGINT:
                final String bs = ((BigInteger)value).toString();
                // Only support bigints whose length can be encoded as a short
                if (bs.length() > Short.MAX_VALUE) {
                    throw new IllegalStateException(String.format("In SampleCoder.encodeScalarValue(), the string length of the BigInteger is %d; too large to be represented in a Short", bs.length()));
                }
                final byte[] bbytes = bs.getBytes("UTF-8");
                outputStream.writeShort(bs.length());
                outputStream.write(bbytes, 0, bbytes.length);
                break;
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

    /**
     * This routine returns a ScalarSample that may have a smaller representation than the
     * ScalarSample argument.  In particular, if tries hard to choose the most compact
     * representation of double-precision values.
     * @param sample A ScalarSample to be compressed
     * @return Either the same ScalarSample is that input, for for some cases of opcode DOUBLE,
     * a more compact ScalarSample which when processed returns a double value.
     */
    @SuppressWarnings("unchecked")
    public static ScalarSample compressSample(final ScalarSample sample) {
        switch (sample.getOpcode()) {
        case INT:
            final int intValue = (Integer)sample.getSampleValue();
            if (intValue == 0) {
                return INT_ZERO_SAMPLE;
            }
            else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
                return new ScalarSample(SampleOpcode.BYTE, new Byte((byte)intValue));
            }
            else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
                return new ScalarSample(SampleOpcode.SHORT, new Short((short)intValue));
            }
            else {
                return sample;
            }
        case LONG:
            final long longValue = (Long)sample.getSampleValue();
            if (longValue == 0) {
                return INT_ZERO_SAMPLE;
            }
            else if (longValue >= Byte.MIN_VALUE && longValue <= Byte.MAX_VALUE) {
                return new ScalarSample(SampleOpcode.BYTE, new Byte((byte)longValue));
            }
            else if (longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE) {
                return new ScalarSample(SampleOpcode.SHORT, new Short((short)longValue));
            }
            else if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                return new ScalarSample(SampleOpcode.INT, new Integer((int)longValue));
            }
            else {
                return sample;
            }
        case BIGINT:
            final BigInteger bigValue = (BigInteger)sample.getSampleValue();
            if (bigValue.compareTo(BIGINTEGER_ZERO_VALUE) == 0) {
                return INT_ZERO_SAMPLE;
            }
            final int digits = 1 + bigValue.bitCount();
            if (digits <= 8) {
                return new ScalarSample(SampleOpcode.BYTE, new Byte((byte)bigValue.intValue()));
            }
            else if (digits <= 16) {
                return new ScalarSample(SampleOpcode.SHORT, new Short((short)bigValue.intValue()));
            }
            else if (digits <= 32) {
                return new ScalarSample(SampleOpcode.INT, new Integer((int)bigValue.intValue()));
            }
            else if (digits <= 64) {
                return new ScalarSample(SampleOpcode.LONG, new Long(bigValue.longValue()));
            }
            else {
                return sample;
            }
        case FLOAT:
            return encodeFloatOrDoubleSample(sample, (double)((Float)sample.getSampleValue()));
        case DOUBLE:
            return encodeFloatOrDoubleSample(sample, (Double)sample.getSampleValue());
        default:
            return sample;
        }
    }

    @SuppressWarnings("unchecked")
    private static ScalarSample encodeFloatOrDoubleSample(final ScalarSample sample, final double value) {
        // We prefer representations in the following order: byte, HalfFloat, short, float and int
        // The criterion for using each representation is the fractional error
        if (value == 0.0) {
            return DOUBLE_ZERO_SAMPLE;
        }
        final boolean integral = value >= MIN_SHORT_DOUBLE_VALUE && value <= MAX_SHORT_DOUBLE_VALUE && (Math.abs((value - (double)((int)value)) / value) <= MAX_FRACTION_ERROR);
        if (integral && value >= MIN_BYTE_DOUBLE_VALUE && value <= MAX_BYTE_DOUBLE_VALUE) {
            return new ScalarSample<Byte>(SampleOpcode.BYTE_FOR_DOUBLE, (byte)value);
        }
        else if (integral && value >= MIN_SHORT_DOUBLE_VALUE && value <= MAX_SHORT_DOUBLE_VALUE) {
            return new ScalarSample<Short>(SampleOpcode.SHORT_FOR_DOUBLE, (short)value);
        }
        else {
            final int halfFloatValue = HalfFloat.fromFloat((float)value);
            if ((Math.abs(value - HalfFloat.toFloat(halfFloatValue)) / value) <= MAX_FRACTION_ERROR) {
                return new ScalarSample<Short>(SampleOpcode.HALF_FLOAT_FOR_DOUBLE, (short)halfFloatValue);
            }
            else if (value >= Float.MIN_VALUE && value <= Float.MAX_VALUE) {
                return new ScalarSample<Float>(SampleOpcode.FLOAT_FOR_DOUBLE, (float)value);
            }
            else {
                return sample;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static double getDoubleValue(final ScalarSample sample) {
        final Object sampleValue = sample.getSampleValue();
        return getDoubleValue(sample.getOpcode(), sampleValue);
    }

    public static double getDoubleValue(final SampleOpcode opcode, final Object sampleValue) {
        switch (opcode) {
        case NULL:
        case DOUBLE_ZERO:
        case INT_ZERO:
            return 0.0;
        case BYTE:
        case BYTE_FOR_DOUBLE:
            return (double)((Byte)sampleValue);
        case SHORT:
        case SHORT_FOR_DOUBLE:
            return (double)((Short)sampleValue);
        case INT:
            return (double)((Integer)sampleValue);
        case LONG:
            return (double)((Long)sampleValue);
        case FLOAT:
        case FLOAT_FOR_DOUBLE:
            return (double)((Float)sampleValue);
        case HALF_FLOAT_FOR_DOUBLE:
            return (double)HalfFloat.toFloat((short)((Short)sampleValue));
        case DOUBLE:
            return (double)((Double)sampleValue);
        case BIGINT:
            return ((BigInteger)sampleValue).doubleValue();
        default:
            throw new IllegalArgumentException(String.format("In getDoubleValue(), sample opcode is %s, sample value is %s",
                    opcode.name(), String.valueOf(sampleValue)));
        }
    }

    public static Object decodeScalarValue(final DataInputStream inputStream, final SampleOpcode opcode) throws IOException {
        switch (opcode) {
        case NULL:
            return null;
        case DOUBLE_ZERO:
            return 0.0;
        case INT_ZERO:
            return 0;
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
        case BIGINT:
            final short bs = inputStream.readShort();
            final byte[] bbytes = new byte[bs];
            final int bbyteCount = inputStream.read(bbytes, 0, bs);
            if (bbyteCount != bs) {
                log.error("Reading bigint came up short");
            }
            return new BigInteger(new String(bbytes, "UTF-8"), 10);
        case BYTE_FOR_DOUBLE:
            return (double)inputStream.readByte();
        case SHORT_FOR_DOUBLE:
            return (double)inputStream.readShort();
        case FLOAT_FOR_DOUBLE:
            final float floatForDouble = inputStream.readFloat();
            return new Double(floatForDouble);
        case HALF_FLOAT_FOR_DOUBLE:
            final float f = HalfFloat.toFloat(inputStream.readShort());
            return new Double(f);
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
        //System.out.printf("Decoded: %s\n", new String(Hex.encodeHex(bytes)));
        final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        final DataInputStream inputStream = new DataInputStream(byteStream);
        final TimeCursor timeCursor = new TimeCursor(timestamps);
        while (true) {
            final byte opcodeByte;
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
                processor.processSamples(timeCursor, repeatCount, repeatedOpcode, value);
                timeCursor.consumeRepeat();
                break;
            default:
                processor.processSamples(timeCursor, 1, opcode, decodeScalarValue(inputStream, opcode));
                break;
            }
        }
    }
}
