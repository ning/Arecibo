package com.ning.arecibo.util.timeline.persistent;

import com.fasterxml.util.membuf.MemBuffersForBytes;
import com.fasterxml.util.membuf.StreamyBytesMemBuffer;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileBackedBuffer
{
    private static final Logger log = LoggerFactory.getLogger(FileBackedBuffer.class);

    private static final SmileFactory smileFactory = new SmileFactory();

    static {
        // yes, full 'compression' by checking for repeating names, short string values:
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_NAMES, true);
        smileFactory.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
    }

    private static final ObjectMapper smileObjectMapper = new ObjectMapper(smileFactory);

    private final StreamyBytesPersistentOutputStream out;
    private final SmileGenerator smileGenerator;

    public FileBackedBuffer(final String basePath, final String prefix) throws IOException
    {
        // This configuration creates ~60M files, this to stay below 64M default limit
        // that JVM has for direct buffers.
        final MemBuffersForBytes bufs = new MemBuffersForBytes(4 * 1024 * 1024, 1, 15);
        final StreamyBytesMemBuffer inputBuffer = bufs.createStreamyBuffer(8, 15);
        out = new StreamyBytesPersistentOutputStream(basePath, prefix, inputBuffer);
        smileGenerator = smileFactory.createJsonGenerator(out, JsonEncoding.UTF8);
    }

    public boolean append(final HostSamplesForTimestamp hostSamplesForTimestamp)
    {
        try {
            smileGenerator.writeStartObject();
            smileGenerator.writeFieldName("event");
            smileObjectMapper.writeValue(smileGenerator, hostSamplesForTimestamp);
            smileGenerator.writeEndObject();

            return true;
        }
        catch (IOException e) {
            log.warn("Unable to backup samples", e);
            return false;
        }
    }

    public void discard()
    {
        for (final String path : out.getCreatedFiles()) {
            try {
                log.info("Discarding file: {}", path);
                Files.deleteIfExists(Paths.get(path));
            }
            catch (IOException e) {
                log.warn("Unable to discard file: {}", path, e);
            }
        }
    }

    public long getFilesCreated()
    {
        return out.getCreatedFiles().size();
    }
}
