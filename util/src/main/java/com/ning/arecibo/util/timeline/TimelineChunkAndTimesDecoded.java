package com.ning.arecibo.util.timeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;

import com.ning.arecibo.util.Logger;

public class TimelineChunkAndTimesDecoded {
    private static final Logger log = Logger.getLogger(TimelineChunkAndTimesDecoded.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TimelineChunkAndTimes chunkAndTimes;

    public TimelineChunkAndTimesDecoded(TimelineChunkAndTimes chunkAndTimes) {
        this.chunkAndTimes = chunkAndTimes;
    }

    @JsonValue
    @Override
    public String toString()
    {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(out);
            generator.writeStartObject();

            generator.writeFieldName("sampleKind");
            generator.writeString(chunkAndTimes.getSampleKind());

            generator.writeFieldName("decodedSamples");
            generator.writeString(getDecodedSamples());

            generator.writeEndObject();
            generator.close();
            return out.toString();
        }
        catch (IOException e) {
            log.error(e);
        }

        return null;
    }

    private String getDecodedSamples() throws IOException {
        final DecodedSampleOutputProcessor processor = new DecodedSampleOutputProcessor();
        SampleCoder.scan(chunkAndTimes.getTimelineChunk().getSamples(), chunkAndTimes.getTimelineTimes(), processor);
        return processor.getDecodedSamples();
    }

    private static final class DecodedSampleOutputProcessor implements SampleProcessor {
        final StringBuilder builder = new StringBuilder();

        @Override
        public void processSamples(TimelineTimes timestamps, int sampleNumber, int sampleCount, SampleOpcode opcode, Object value) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            final DateTime timestamp = timestamps.getSampleTimestamp(sampleNumber);
            builder.append("at ").append(timestamp.toString("yyyy-MM-dd HH:mm:ss")).append(" ");
            if (sampleCount > 1) {
                builder.append(sampleCount).append(" of ");
            }
            builder.append(opcode.name().toLowerCase());
            switch (opcode) {
            case NULL:
            case DOUBLE_ZERO:
            case INT_ZERO:
                break;
            default:
                builder.append("(").append(String.valueOf(value)).append(")");
                break;
            }
        }

        public String getDecodedSamples() {
            return builder.toString();
        }
    }
}