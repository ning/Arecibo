package com.ning.arecibo.dashboard.client.datahierarchy.results;

import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyResultType;

public class DataHierarchyResult {

    final private DataHierarchyResultType resultType;

    public DataHierarchyResult(DataHierarchyResultType resultType) {
        this.resultType = resultType;
    }

    public DataHierarchyResultType getResponseType() {
        return resultType;
    }
}
