package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;


public class AcknowledgementLogByAckTimeComparator implements Comparator<ConfDataAcknowledgementLog> {

    static final AcknowledgementLogByAckTimeComparator instance = new AcknowledgementLogByAckTimeComparator();

    public static AcknowledgementLogByAckTimeComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataAcknowledgementLog o1, ConfDataAcknowledgementLog o2) {
        return o1.getAckTime().compareTo(o2.getAckTime());
    }
}
