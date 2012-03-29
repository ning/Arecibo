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

package com.ning.arecibo.agent.config;

import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;
import com.ning.arecibo.util.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConfigStreamReader extends BaseConfigReader
{
    private static final Logger log = Logger.getLogger(BaseConfigReader.class);

    public static final String INCLUDES = "includes";
    public static final String CONFIGS = "configs";

    private final List<Config> configs = new ArrayList<Config>();
    private final List<String> includeFilesToProcess = new ArrayList<String>();

    public ConfigStreamReader(Reader inReader, final String host, final String configPath, final String coreType,
                              final GuiceDefaultsForDataSources guiceDefaults) throws ConfigException
    {
        super(host, configPath, coreType, guiceDefaults);

        parseConfigsFromJSON(inReader);

        final ClassLoader cLoader = Thread.currentThread().getContextClassLoader();
        while (includeFilesToProcess.size() > 0) {
            final String includeFile = includeFilesToProcess.remove(0);

            // make sure we don't duplicate any configs (prevent circular reference loop)
            final List<String> includeFilesProcessed = new ArrayList<String>();
            if (includeFilesProcessed.contains(includeFile)) {
                continue;
            }

            final InputStream inputStream = cLoader.getResourceAsStream(includeFile);
            includeFilesProcessed.add(includeFile);

            if (inputStream != null) {
                log.info("Loading config entries from included file '%s'", includeFile);
            }
            else {
                throw new ConfigException("Could not load included config file '" + includeFile + "'");
            }

            inReader = new InputStreamReader(inputStream);
            parseConfigsFromJSON(inReader);
        }
    }

    private void parseConfigsFromJSON(final Reader inReader) throws ConfigException
    {
        JsonParser parser = null;
        try {
            parser = new MappingJsonFactory().createJsonParser(inReader);
            parser.enable(JsonParser.Feature.ALLOW_COMMENTS);
            parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
            parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

            final JsonNode rootNode = parser.readValueAsTree();

            // add include file references to list to process later
            final JsonNode includesNode = rootNode.path(INCLUDES);
            if (!includesNode.isMissingNode() && includesNode.size() > 0) {

                for (final JsonNode includeNode : includesNode) {
                    final String fileName = includeNode.asText();
                    includeFilesToProcess.add(fileName);
                }
            }

            // process configs
            final JsonNode configsNode = rootNode.path(CONFIGS);
            if (!configsNode.isMissingNode() && configsNode.size() > 0) {

                for (final JsonNode configNode : configsNode) {
                    final Map<String, Object> fieldMap = new HashMap<String, Object>();

                    final Iterator<String> fieldNames = configNode.fieldNames();
                    while (fieldNames.hasNext()) {
                        final String fieldName = fieldNames.next();
                        final JsonNode valueNode = configNode.get(fieldName);

                        // allow one level of sub-nodes, and return list of objects
                        if (valueNode.isArray()) {
                            final Iterator<JsonNode> arrayNodes = valueNode.elements();
                            final ArrayList<Object> arrayValues = new ArrayList<Object>();
                            while (arrayNodes.hasNext()) {
                                final JsonNode arrayNode = arrayNodes.next();
                                arrayValues.add(arrayNode.asText());
                            }
                            fieldMap.put(fieldName, arrayValues);
                        }
                        else if (valueNode.isNumber()) {
                            fieldMap.put(fieldName, valueNode.numberValue());
                        }
                        else {
                            fieldMap.put(fieldName, valueNode.asText());
                        }
                    }

                    final Config config = createConfigFromMap(fieldMap);

                    if (config != null) {
                        configs.add(config);
                    }
                }
            }
        }
        catch (IOException ioEx) {
            throw new ConfigException("problem creating json parser from inputStream", ioEx);
        }
        catch (RuntimeException ruEx) {
            log.warn(ruEx);
            throw new ConfigException("problem creating json parser from inputStream", ruEx);
        }
        finally {
            try {
                if (parser != null) {
                    parser.close();
                }
                if (inReader != null) {
                    inReader.close();
                }
            }
            catch (IOException ioEx) {
                log.info(ioEx, "IOException:");
                // return ok here
            }
        }
    }

    public List<Config> getConfigurations()
    {
        return this.configs;
    }
}
