package com.ning.arecibo.alertmanager.utils.comparators;

import java.util.Comparator;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;


public class NotificationConfigByAddressComparator implements Comparator<ConfDataNotifConfig> {

    static final NotificationConfigByAddressComparator instance = new NotificationConfigByAddressComparator();

    public static NotificationConfigByAddressComparator getInstance() {
        return instance;
    }

    public int compare(ConfDataNotifConfig o1, ConfDataNotifConfig o2) {
        return o1.getAddress().compareTo(o2.getAddress());
    }
}
