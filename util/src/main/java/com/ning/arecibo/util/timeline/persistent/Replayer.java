package com.ning.arecibo.util.timeline.persistent;

import com.google.common.base.Function;
import com.ning.arecibo.util.timeline.HostSamplesForTimestamp;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.smile.SmileFactory;
import org.codehaus.jackson.smile.SmileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Replayer
{
    private static final Logger log = LoggerFactory.getLogger(Replayer.class);
    private static final SmileFactory smileFactory = new SmileFactory();
    private static final ObjectMapper smileMapper = new ObjectMapper(smileFactory);

    static {
        smileFactory.configure(SmileParser.Feature.REQUIRE_HEADER, false);
        smileFactory.setCodec(smileMapper);
    }

    private final String path;

    public Replayer(final String path)
    {
        this.path = path;
    }

    public List<HostSamplesForTimestamp> readAll()
    {
        final List<HostSamplesForTimestamp> samples = new ArrayList<HostSamplesForTimestamp>();

        readAll(new Function<HostSamplesForTimestamp, Void>()
        {
            @Override
            public Void apply(@Nullable final HostSamplesForTimestamp input)
            {
                if (input != null) {
                    samples.add(input);
                }
                return null;
            }
        });

        return samples;
    }

    public void readAll(final Function<HostSamplesForTimestamp, Void> fn)
    {
        for (final File file : FileUtils.listFiles(new File(path), new String[]{"bin"}, false)) {
            try {
                read(file, fn);

                if (!file.delete()) {
                    log.warn("Unable to delete file: {}", file.getAbsolutePath());
                }
            }
            catch (IOException e) {
                log.warn("Exception replaying file: {}", file.getAbsolutePath(), e);
            }
        }
    }

    private void read(final File file, final Function<HostSamplesForTimestamp, Void> fn) throws IOException
    {
        final JsonParser smileParser = smileFactory.createJsonParser(file);
        if (smileParser.nextToken() != JsonToken.START_ARRAY) {
            return;
        }

        while (smileParser.nextToken() != JsonToken.END_ARRAY) {
            final HostSamplesForTimestamp hostSamplesForTimestamp = smileParser.readValueAs(HostSamplesForTimestamp.class);
            fn.apply(hostSamplesForTimestamp);
        }

        smileParser.close();
    }
}