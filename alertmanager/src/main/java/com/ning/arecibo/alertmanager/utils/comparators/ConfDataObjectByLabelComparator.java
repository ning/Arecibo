package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;


public class ConfDataObjectByLabelComparator<T extends ConfDataObject> implements Comparator<T> {

    static final ConfDataObjectByLabelComparator instance = new ConfDataObjectByLabelComparator();

    public static ConfDataObjectByLabelComparator getInstance() {
        return instance;
    }

    public int compare(T o1, T o2) {
        return o1.getLabel().compareTo(o2.getLabel());
    }
}
