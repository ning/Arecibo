package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

public class NotifGroupExtendedForDisplay extends ConfDataNotifGroup {

    private volatile String extendedNotificationConfigs = null;

    public NotifGroupExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        NotifGroupExtendedForDisplay extendedDataObject = (NotifGroupExtendedForDisplay) dataObject;
        if(!checkFilterMatch(extendedDataObject.getExtendedNotificationConfigs(),this.extendedNotificationConfigs))
            return false;

        return true;
    }

    public String getExtendedNotificationConfigs() {
        return extendedNotificationConfigs;
    }

    public void setExtendedNotificationConfigs(String extendedNotificationConfigs) {
        this.extendedNotificationConfigs = extendedNotificationConfigs;
    }
}
