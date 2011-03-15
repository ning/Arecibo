package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;


public class ThresholdQualifyingAttrByAttributeTypeComparator implements Comparator<ConfDataThresholdQualifyingAttr> {

    static final ThresholdQualifyingAttrByAttributeTypeComparator instance = new ThresholdQualifyingAttrByAttributeTypeComparator();

    public static ThresholdQualifyingAttrByAttributeTypeComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataThresholdQualifyingAttr o1, ConfDataThresholdQualifyingAttr o2) {
        return o1.getAttributeType().compareTo(o2.getAttributeType());
    }
}
