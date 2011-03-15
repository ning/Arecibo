package com.ning.arecibo.dashboard.client.datahierarchy.serializers;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyPathComponentsResult;

public class DataHierarchyPathComponentsResultSerializer extends DataHierarchyResultSerializer {

    public final static String HIERARCHY_PATH_COMPONENTS = "hierarchy-path-components";
    public final static String HIERARCHY_PATH_COMPONENT = "hierarchy-path-component";

    public static void serializeHierarchyResultToJSON(DataHierarchyPathComponentsResult result,JsonGenerator out)
            throws IOException {

        out.writeStartObject();

        out.writeFieldName(TYPE);
        out.writeString(result.getResponseType().toString());

        out.writeFieldName(HIERARCHY_PATH_COMPONENTS);
        out.writeStartArray();

        List<String> options = result.getPathComponents();
        for(String option:options) {
            out.writeStartObject();

            out.writeFieldName(TYPE);
            out.writeString(HIERARCHY_PATH_COMPONENT);

            out.writeFieldName(VALUE);
            out.writeString(option);

            out.writeEndObject();
        }

        out.writeEndArray();

        out.writeEndObject();
    }
}
