package com.ning.arecibo.dashboard.client.datahierarchy.types;

public class DataUrlParamSpec {
    final String name;
    final DataUrlDataType dataType;
    final Object defaultValue;
    final Boolean required;
    final String description;

    public DataUrlParamSpec(String name,
                            DataUrlDataType dataType,
                            Object defaultValue,
                            Boolean required,
                            String description) {
        this.name = name;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.required = required;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public DataUrlDataType getDataType() {
        return dataType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }
}
