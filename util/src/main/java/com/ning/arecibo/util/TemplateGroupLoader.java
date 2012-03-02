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
