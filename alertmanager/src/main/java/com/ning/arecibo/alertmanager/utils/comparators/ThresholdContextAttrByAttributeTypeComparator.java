package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;


public class ThresholdContextAttrByAttributeTypeComparator implements Comparator<ConfDataThresholdContextAttr> {

    static final ThresholdContextAttrByAttributeTypeComparator instance = new ThresholdContextAttrByAttributeTypeComparator();

    public static ThresholdContextAttrByAttributeTypeComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataThresholdContextAttr o1, ConfDataThresholdContextAttr o2) {
        return o1.getAttributeType().compareTo(o2.getAttributeType());
    }
}
