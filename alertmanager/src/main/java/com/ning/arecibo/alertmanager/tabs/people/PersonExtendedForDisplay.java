package com.ning.arecibo.alertmanager.tabs.people;

import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;

public class PersonExtendedForDisplay extends ConfDataPerson {

    private volatile String extendedNotificationConfig = null;

    public PersonExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        PersonExtendedForDisplay extendedDataObject = (PersonExtendedForDisplay) dataObject;
        if(!checkFilterMatch(extendedDataObject.getExtendedNotificationConfig(),this.extendedNotificationConfig))
            return false;
        
        return true;
    }

    public String getExtendedNotificationConfig() {
        return extendedNotificationConfig;
    }

    public void setExtendedNotificationConfig(String extendedNotificationConfig) {
        this.extendedNotificationConfig = extendedNotificationConfig;
    }
}
