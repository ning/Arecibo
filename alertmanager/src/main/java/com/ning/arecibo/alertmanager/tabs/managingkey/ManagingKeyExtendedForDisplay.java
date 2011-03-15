package com.ning.arecibo.alertmanager.tabs.managingkey;

import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

public class ManagingKeyExtendedForDisplay extends ConfDataManagingKey {

    private volatile String extendedScheduleByTimeOfDay = null;
    private volatile String extendedScheduleByDayOfWeek = null;
    private volatile String extendedManualInvocationSettings = null;

    public ManagingKeyExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        ManagingKeyExtendedForDisplay extendedDataObject = (ManagingKeyExtendedForDisplay) dataObject;

        if(!checkFilterMatch(extendedDataObject.getExtendedScheduleByTimeOfDay(),this.extendedScheduleByTimeOfDay))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedScheduleByDayOfWeek(),this.extendedScheduleByDayOfWeek))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedManualInvocationSettings(),this.extendedManualInvocationSettings))
            return false;

        return true;
    }

    public String getExtendedScheduleByTimeOfDay() {
        return extendedScheduleByTimeOfDay;
    }

    public void setExtendedScheduleByTimeOfDay(String extendedScheduleByTimeOfDay) {
        this.extendedScheduleByTimeOfDay = extendedScheduleByTimeOfDay;
    }

    public String getExtendedScheduleByDayOfWeek() {
        return extendedScheduleByDayOfWeek;
    }

    public void setExtendedScheduleByDayOfWeek(String extendedScheduleByDayOfWeek) {
        this.extendedScheduleByDayOfWeek = extendedScheduleByDayOfWeek;
    }

    public String getExtendedManualInvocationSettings() {
        return extendedManualInvocationSettings;
    }

    public void setExtendedManualInvocationSettings(String extendedManualInvocationSettings) {
        this.extendedManualInvocationSettings = extendedManualInvocationSettings;
    }
}
