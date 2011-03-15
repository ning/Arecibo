package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;


public class ConfDataObjectByIdComparator<T extends ConfDataObject> implements Comparator<T> {

    static final ConfDataObjectByIdComparator instance = new ConfDataObjectByIdComparator();

    public static ConfDataObjectByIdComparator getInstance() {
        return instance;
    }

    public int compare(T o1, T o2) {
        return o1.getId().compareTo(o2.getId());
    }
}
