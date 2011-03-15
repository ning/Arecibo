package com.ning.arecibo.dashboard.client.datahierarchy.results;

import java.util.List;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyResultType;


public class DataHierarchyPathComponentsResult extends DataHierarchyResult {

    final private List<String> pathComponents;

    public DataHierarchyPathComponentsResult(DataHierarchyResultType resultType,List<String> pathComponents) {
        super(resultType);
        this.pathComponents = pathComponents;
    }

    public List<String> getPathComponents() {
        return this.pathComponents;
    }
}
