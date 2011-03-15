package com.ning.arecibo.alertmanager.tabs.alertincidents;

import com.ning.arecibo.alert.confdata.objects.ConfDataAlertIncidentLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

public class AlertIncidentLogExtendedForDisplay extends ConfDataAlertIncidentLog {

    private volatile String extendedThresholdConfigName = null;
    private volatile String extendedContextData = null;
    private volatile String extendedAcknowledgementLogs = null;

    public AlertIncidentLogExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        AlertIncidentLogExtendedForDisplay extendedDataObject = (AlertIncidentLogExtendedForDisplay) dataObject;

        if(!checkFilterMatch(extendedDataObject.getExtendedThresholdConfigName(),this.extendedThresholdConfigName))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedContextData(),this.extendedContextData))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedAcknowledgementLogs(),this.extendedAcknowledgementLogs))
            return false;

        return true;
    }

    public String getExtendedThresholdConfigName() {
        return extendedThresholdConfigName;
    }

    public void setExtendedThresholdConfigName(String extendedThresholdConfigName) {
        this.extendedThresholdConfigName = extendedThresholdConfigName;
    }

    public String getExtendedContextData() {
        return extendedContextData;
    }

    public void setExtendedContextData(String extendedContextData) {
        this.extendedContextData = extendedContextData;
    }

    public String getExtendedAcknowledgementLogs() {
        return extendedAcknowledgementLogs;
    }

    public void setExtendedAcknowledgementLogs(String extendedAcknowledgementLogs) {
        this.extendedAcknowledgementLogs = extendedAcknowledgementLogs;
    }
}
