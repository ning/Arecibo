package com.ning.arecibo.dashboard.client.datahierarchy.results;

import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyResultType;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataUrlSpec;

public class DataHierarchyDataSpecResult extends DataHierarchyResult {

    final private DataUrlSpec urlSpec;

    public DataHierarchyDataSpecResult(DataUrlSpec urlSpec) {
        super(DataHierarchyResultType.DATA_SPEC);
        this.urlSpec = urlSpec;
    }

    public DataUrlSpec getUrlSpec() {
        return urlSpec;
    }
}
