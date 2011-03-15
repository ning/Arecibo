package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;


public class ConfDataObjectByUpdateTimestampComparator<T extends ConfDataObject> implements Comparator<T> {

    static final ConfDataObjectByUpdateTimestampComparator instance = new ConfDataObjectByUpdateTimestampComparator();

    public static ConfDataObjectByUpdateTimestampComparator getInstance() {
        return instance;
    }

    public int compare(T o1, T o2) {
        return o1.getUpdateTimestamp().compareTo(o2.getUpdateTimestamp());
    }
}
