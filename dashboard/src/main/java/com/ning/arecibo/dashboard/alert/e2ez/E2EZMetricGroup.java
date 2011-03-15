package com.ning.arecibo.dashboard.alert.e2ez;

import java.util.List;
import java.util.ArrayList;

public class E2EZMetricGroup implements E2EZNode {

    private final String groupName;
    private final int warnThreshold;
    private final int criticalThreshold;
    private final List<E2EZNode> children = new ArrayList<E2EZNode>();

    public E2EZMetricGroup(String groupName,int warnThreshold,int criticalThreshold) {

        if(groupName == null)
            throw new IllegalStateException("groupName cannot be null");

        this.groupName = groupName;
        this.warnThreshold = warnThreshold;
        this.criticalThreshold = criticalThreshold;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    public int getWarnThreshold() {
        return warnThreshold;
    }

    public void addChildNode(E2EZNode node) {
        children.add(node);
    }

    public List<E2EZNode> getChildren() {
        return children;
    }
}
