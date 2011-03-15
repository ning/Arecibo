package com.ning.arecibo.dashboard.client.datahierarchy.serializers;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import com.ning.arecibo.dashboard.client.datahierarchy.results.DataHierarchyDataSpecResult;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlParamSpec;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlSpec;

public class DataHierarchyDataSpecResultSerializer extends DataHierarchyResultSerializer {

    public final static String DATA_HOST = "data-host";
    public final static String DATA_PATH = "data-path";
    public final static String BASE_QUERY_STRING = "base-query-string";
    public final static String OPTIONAL_QUERY_PARAMS = "optional-query-params";
    public final static String QUERY_PARAM = "query-param";
    public final static String NAME = "name";
    public final static String DATA_TYPE = "data-type";
    public final static String REQUIRED = "required";
    public final static String DEFAULT_VALUE = "default_value";
    public final static String DESCRIPTION = "description";

    public static void serializeHierarchyResultToJSON(DataHierarchyDataSpecResult result,JsonGenerator out)
            throws IOException {

        DataUrlSpec urlSpec = result.getUrlSpec();

        out.writeStartObject();

        out.writeFieldName(TYPE);
        out.writeString(result.getResponseType().toString());

        out.writeFieldName(DESCRIPTION);
        out.writeString(urlSpec.getDescription()); 

        out.writeFieldName(DATA_HOST);
        out.writeString(urlSpec.getDataHost());

        out.writeFieldName(DATA_PATH);
        out.writeString(urlSpec.getDataPath());

        out.writeFieldName(BASE_QUERY_STRING);
        out.writeString(urlSpec.getBaseQueryParameterString());

        out.writeFieldName(OPTIONAL_QUERY_PARAMS);
        out.writeStartArray();

        List<DataUrlParamSpec> optionalParams = urlSpec.getOptionalQueryParameters();
        if(optionalParams != null) {
            for(DataUrlParamSpec optionalParam:optionalParams) {
                out.writeStartObject();

                out.writeFieldName(TYPE);
                out.writeString(QUERY_PARAM);

                out.writeFieldName(NAME);
                out.writeString(optionalParam.getName());

                out.writeFieldName(DATA_TYPE);
                out.writeString(optionalParam.getDataType().toString());

                if(optionalParam.getDefaultValue() != null) {
                    out.writeFieldName(DEFAULT_VALUE);
                    out.writeString(optionalParam.getDefaultValue().toString());
                }

                out.writeFieldName(REQUIRED);
                out.writeBoolean(optionalParam.isRequired());

                if(optionalParam.getDescription() != null) {
                    out.writeFieldName(DESCRIPTION);
                    out.writeString(optionalParam.getDescription());
                }

                out.writeEndObject();
            }
        }

        out.writeEndArray();
        
        out.writeEndObject();
    }
}
