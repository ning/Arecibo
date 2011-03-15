package com.ning.arecibo.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;

public class TemplateGroupLoader
{
    private static final Logger log = Logger.getLogger(TemplateGroupLoader.class);

    public static StringTemplateGroup load(Class<?> classObj, String resourceName)
    {
        final String name = classObj.getPackage().getName().replace(".", "/") + resourceName;
        final URL resourceUrl = TemplateGroupLoader.class.getClassLoader().getResource(name);
        if (resourceUrl == null) {
            throw new RuntimeException(String.format("Error loading StringTemplate: Resource %s does not exist!", name));
        }
        Reader reader;

        try {
            reader = new InputStreamReader(resourceUrl.openStream());
        }
        catch (IOException ex) {
            throw new RuntimeException(String.format("Error loading StringTemplate: %s", name), ex);
        }

        AtomicBoolean error = new AtomicBoolean(false);

        StringTemplateGroup result = new StringTemplateGroup(reader, AngleBracketTemplateLexer.class, new StringTemplateErrorListener() {
            @Override
            public void error(String msg, Throwable e)
            {
                log.error(msg, e);
            }

            @Override
            public void warning(String msg)
            {
                log.warn(msg);
            }
        });

        if (error.get()) {
            throw new RuntimeException(String.format("Error loading StringTemplate: %s", name));
        }

        return result;
    }
}
