package com.ning.arecibo.collector.dao;

// Note these should be injected rather than hard-coded here
public enum AggregationType
{
    HOST(":insert_host_event", "HOST_EVENTS", true),
    PATH(":insert_path_event", "PATH_EVENTS", true),
    TYPE(":insert_type_event", "TYPE_EVENTS", true),
    GENERIC(":insert_generic_event", "GENERIC_EVENTS", false);

    private final String templateName;
    private final String baseTableName;
    private final boolean supportsMultiRes;

    AggregationType(String templateName, String baseTableName, boolean supportsMultiRes)
    {
        this.templateName = templateName;
        this.baseTableName = baseTableName;
        this.supportsMultiRes = supportsMultiRes;
    }

    public String getTemplateName()
    {
        return this.templateName;
    }

    public String getBaseTableName()
    {
        return this.baseTableName;
    }

    public boolean getSupportsMultiRes()
    {
        return this.supportsMultiRes;
    }
}
