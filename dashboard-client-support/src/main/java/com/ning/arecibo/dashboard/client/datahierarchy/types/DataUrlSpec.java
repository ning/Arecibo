package com.ning.arecibo.dashboard.client.datahierarchy.types;

import java.util.List;

public class DataUrlSpec {

    private final String dataHost;
    private final String dataPath;
    private final String baseQueryParameterString;
    private final String description;
    private final List<DataUrlParamSpec> optionalQueryParameters;

    public DataUrlSpec(String dataHost,
                                    String dataPath,
                                    String baseQueryParameterString,
                                    String description,
                                    List<DataUrlParamSpec> optionalQueryParameters) {

        this.dataHost = dataHost;
        this.dataPath = dataPath;
        this.baseQueryParameterString = baseQueryParameterString;
        this.description = description;
        this.optionalQueryParameters = optionalQueryParameters;
    }

    public String getBaseQueryParameterString() {
        return baseQueryParameterString;
    }

    public String getDataHost() {
        return dataHost;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getDescription() {
        return description;
    }

    public List<DataUrlParamSpec> getOptionalQueryParameters() {
        return optionalQueryParameters;
    }
}
