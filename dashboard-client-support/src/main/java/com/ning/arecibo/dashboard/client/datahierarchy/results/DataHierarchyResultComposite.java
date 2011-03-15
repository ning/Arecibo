package com.ning.arecibo.dashboard.client.datahierarchy.results;

import java.util.List;
import java.util.ArrayList;
import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyResultType;


public class DataHierarchyResultComposite extends DataHierarchyResult {

    private final List<DataHierarchyResult> results;

    public DataHierarchyResultComposite() {
        super(DataHierarchyResultType.COMPOSITE);
        results = new ArrayList<DataHierarchyResult>();
    }

    public void addResult(DataHierarchyResult result) {
        results.add(result);
    }

    public List<DataHierarchyResult> getResults() {
        return results;
    }
}
