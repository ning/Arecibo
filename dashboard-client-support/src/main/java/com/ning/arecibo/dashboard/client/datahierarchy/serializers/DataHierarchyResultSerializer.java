package com.ning.arecibo.dashboard.client.datahierarchy.serializers;

import java.io.StringWriter;
import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyDataSpecResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyPathComponentsResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyResult;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyResultComposite;

public class DataHierarchyResultSerializer {

    public final static String DATA_HIERARCHY_RESULTS = "data_hierarchy_results";
    public final static String TYPE = "type";
    public final static String VALUE = "value";

    public static String serializeHierarchyResultToJSON(DataHierarchyResult dResult) throws IOException {

        // do setup, and write header info
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter sw = new StringWriter();

        JsonGenerator out = jsonFactory.createJsonGenerator(sw);
        out.setPrettyPrinter(new DefaultPrettyPrinter());

        out.writeStartObject();
        out.writeFieldName(DATA_HIERARCHY_RESULTS);
        out.writeStartArray();

        serializeResult(dResult,out);

        out.writeEndArray();
        out.writeEndObject();

        // flush and return result
        out.flush();
        return sw.toString();
    }

    private static void serializeResult(DataHierarchyResult dResult,JsonGenerator out) throws IOException {

        switch(dResult.getResponseType()) {
            case HIERARCHY_PATH_COMPONENTS:
                DataHierarchyPathComponentsResultSerializer.serializeHierarchyResultToJSON(
                        (DataHierarchyPathComponentsResult)dResult,out);
                break;
            case DATA_SPEC:
                DataHierarchyDataSpecResultSerializer.serializeHierarchyResultToJSON(
                        (DataHierarchyDataSpecResult)dResult,out);
                break;
            case COMPOSITE:
                List<DataHierarchyResult> results = ((DataHierarchyResultComposite)dResult).getResults();
                for(DataHierarchyResult result:results) {
                    serializeResult(result,out);
                }
                break;
            default:
                break;
        }
    }
}
