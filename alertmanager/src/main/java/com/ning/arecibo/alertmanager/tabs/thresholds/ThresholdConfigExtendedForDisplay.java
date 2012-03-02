/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.alertmanager.tabs.thresholds;

import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;

public class ThresholdConfigExtendedForDisplay extends ConfDataThresholdConfig {

    private volatile String extendedAlertingConfig = null;
    private volatile String extendedContextAttributes = null;
    private volatile String extendedQualifyingAttributes = null;

    public ThresholdConfigExtendedForDisplay() {
        super();
    }

    @Override
    public boolean filterMatchesDataObject(ConfDataObject dataObject) {
        boolean superResult = super.filterMatchesDataObject(dataObject);

        if(!superResult)
            return false;

        // compare extended fields
        ThresholdConfigExtendedForDisplay extendedDataObject = (ThresholdConfigExtendedForDisplay) dataObject;
        if(!checkFilterMatch(extendedDataObject.getExtendedAlertingConfig(),this.extendedAlertingConfig))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedContextAttributes(),this.extendedContextAttributes))
            return false;

        if(!checkFilterMatch(extendedDataObject.getExtendedQualifyingAttributes(),this.extendedQualifyingAttributes))
            return false;
        
        return true;
    }

    public String getExtendedAlertingConfig() {
        return this.extendedAlertingConfig;
    }

    public void setExtendedAlertingConfig(String extendedAlertingConfig) {
        this.extendedAlertingConfig = extendedAlertingConfig;
    }

    public String getExtendedContextAttributes() {
        return extendedContextAttributes;
    }

    public void setExtendedContextAttributes(String extendedContextAttributes) {
        this.extendedContextAttributes = extendedContextAttributes;
    }

    public String getExtendedQualifyingAttributes() {
        return extendedQualifyingAttributes;
    }

    public void setExtendedQualifyingAttributes(String extendedQualifyingAttributes) {
        this.extendedQualifyingAttributes = extendedQualifyingAttributes;
    }

}
