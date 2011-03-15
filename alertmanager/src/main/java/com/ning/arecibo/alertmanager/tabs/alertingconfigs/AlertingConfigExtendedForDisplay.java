package com.ning.arecibo.alertmanager.tabs.alertingconfigs;

import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

public class AlertingConfigExtendedForDisplay extends ConfDataAlertingConfig {

    private volatile String extendedNotificationGroups = null;
    private volatile String extendedManagingKeys = null;

    public AlertingConfigExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        AlertingConfigExtendedForDisplay extendedDataObject = (AlertingConfigExtendedForDisplay) dataObject;

        if(!checkFilterMatch(extendedDataObject.getExtendedNotificationGroups(),this.extendedNotificationGroups))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedManagingKeys(),this.extendedManagingKeys))
            return false;

        return true;
    }

    public String getExtendedNotificationGroups() {
        return extendedNotificationGroups;
    }

    public void setExtendedNotificationGroups(String extendedNotificationGroups) {
        this.extendedNotificationGroups = extendedNotificationGroups;
    }

    public String getExtendedManagingKeys() {
        return extendedManagingKeys;
    }

    public void setExtendedManagingKeys(String extendedManagingKeys) {
        this.extendedManagingKeys = extendedManagingKeys;
    }
}
