package com.ning.arecibo.agent.config;

import com.ning.arecibo.util.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public class ConfigStreamReader extends BaseConfigReader
{
	private static final Logger log = Logger.getLogger(BaseConfigReader.class);

    public final static String INCLUDES = "includes";
    public final static String CONFIGS = "configs";

	private final List<Config> configs = new ArrayList<Config>();
    private final List<String> includeFilesToProcess = new ArrayList<String>();
    private final List<String> includeFilesProcessed = new ArrayList<String>();

	public ConfigStreamReader(Reader inReader,String host, String configPath, String coreType,
                              GuiceDefaultsForDataSources guiceDefaults) throws ConfigException
	{
		super(host, configPath, coreType, guiceDefaults);

        parseConfigsFromJSON(inReader);

        ClassLoader cLoader = Thread.currentThread().getContextClassLoader();
        while(includeFilesToProcess.size() > 0) {
            String includeFile = includeFilesToProcess.remove(0);

            // make sure we don't duplicate any configs (prevent circular reference loop)
            if(includeFilesProcessed.contains(includeFile))
                continue;

            InputStream inputStream = cLoader.getResourceAsStream(includeFile);
            includeFilesProcessed.add(includeFile);

            if (inputStream != null) {
                log.info("Loading config entries from included file '%s'",includeFile);
            }
            else {
                throw new ConfigException("Could not load included config file '" + includeFile + "'");
            }

            inReader = new InputStreamReader(inputStream);
            parseConfigsFromJSON(inReader);
        }
    }

    private void parseConfigsFromJSON(Reader inReader) throws ConfigException {

        JsonParser parser = null;
        try {
            parser = new MappingJsonFactory().createJsonParser(inReader);
            parser.enable(JsonParser.Feature.ALLOW_COMMENTS);
            parser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
            parser.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);

            JsonNode rootNode = parser.readValueAsTree();

            // add include file references to list to process later
            JsonNode includesNode = rootNode.path(INCLUDES);
            if(!includesNode.isMissingNode() && includesNode.size() > 0) {
                Iterator<JsonNode> includeNodes = includesNode.getElements();

                while(includeNodes.hasNext()) {
                    JsonNode includeNode = includeNodes.next();
                    String fileName = includeNode.getValueAsText();
                    includeFilesToProcess.add(fileName);
                }
            }

            // process configs
            JsonNode configsNode = rootNode.path(CONFIGS);
            if(!configsNode.isMissingNode() && configsNode.size() > 0) {
                Iterator<JsonNode> configNodes = configsNode.getElements();

                while(configNodes.hasNext()) {
                    JsonNode configNode = configNodes.next();
                    Map<String,Object> fieldMap = new HashMap<String,Object>();

                    Iterator<String> fieldNames = configNode.getFieldNames();
                    while(fieldNames.hasNext()) {
                        String fieldName = fieldNames.next();
                        JsonNode valueNode = configNode.get(fieldName);

                        // allow one level of sub-nodes, and return list of objects
                        if(valueNode.isArray()) {
                            Iterator<JsonNode> arrayNodes = valueNode.getElements();
                            ArrayList<Object> arrayValues = new ArrayList<Object>();
                            while(arrayNodes.hasNext()) {
                                JsonNode arrayNode = arrayNodes.next();
                                arrayValues.add(arrayNode.getValueAsText());
                            }
                            fieldMap.put(fieldName,arrayValues);
                        }
                        else if(valueNode.isNumber()) {
                            fieldMap.put(fieldName,valueNode.getNumberValue());
                        }
                        else {
                            fieldMap.put(fieldName,valueNode.getValueAsText());
                        }
                    }

                    Config config = createConfigFromMap(fieldMap);

                    if(config != null)
                        configs.add(config);
                }
            }
        }
        catch(IOException ioEx) {
            throw new ConfigException("problem creating json parser from inputStream",ioEx);
        }
        catch(RuntimeException ruEx) {
            log.warn(ruEx);
            throw new ConfigException("problem creating json parser from inputStream",ruEx);
        }
        finally {
            try {
                if(parser != null)
                    parser.close();
                if(inReader != null)
                    inReader.close();
            }
            catch(IOException ioEx) {
                log.info(ioEx,"IOException:");
                // return ok here
            }
        }
	}

	public List<Config> getConfigurations()
	{
		return this.configs;
	}
}
