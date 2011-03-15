package com.ning.arecibo.dashboard.alert.e2ez;

import java.util.List;
import java.util.ArrayList;

public class E2EZMetricGroupCategory implements E2EZNode {

    private final String categoryName;
    private final List<E2EZNode> children = new ArrayList<E2EZNode>();

    public E2EZMetricGroupCategory(String categoryName) {

        if(categoryName == null)
            throw new IllegalStateException("categoryName cannot be null");

        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void addChildNode(E2EZNode node) {
        children.add(node);
    }

    public List<E2EZNode> getChildren() {
        return children;
    }
}
