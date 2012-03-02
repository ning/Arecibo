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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.apache.wicket.IClusterable;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.alertmanager.utils.ConfDataFormModel;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByIdComparator;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByLabelComparator;

import com.ning.arecibo.util.Logger;


public class ThresholdFormModel extends ConfDataThresholdConfig implements ConfDataFormModel, IClusterable {
    final static Logger log = Logger.getLogger(ThresholdFormModel.class);

    private volatile String lastMessage = null;

    private final List<ConfDataThresholdQualifyingAttr> qualifyingAttrs;
    private final List<ConfDataThresholdContextAttr> contextAttrs;
    private final List<ConfDataAlertingConfig> allAlertingConfigsByLabel;
    private final List<ConfDataAlertingConfig> allAlertingConfigsById;
    private final List<String> allAlertingConfigNames;

    public ThresholdFormModel() {
        this(null);
    }

    public ThresholdFormModel(ConfDataThresholdConfig confDataThresholdConfig) {

        if(confDataThresholdConfig != null)
            this.setPropertiesFromMap(confDataThresholdConfig.toPropertiesMap());

        this.allAlertingConfigNames = new ArrayList<String>();
        this.allAlertingConfigsByLabel = new ArrayList<ConfDataAlertingConfig>();
        this.allAlertingConfigsById = new ArrayList<ConfDataAlertingConfig>();
        this.qualifyingAttrs = new ArrayList<ConfDataThresholdQualifyingAttr>();
        this.contextAttrs = new ArrayList<ConfDataThresholdContextAttr>();

        if(confDataThresholdConfig == null) {
            // some useful defaults for new configs
            this.setMinThresholdSamples(1L);
            this.setClearingIntervalMs(300000L);

            // pre-populate 'hostName' contextAttr (not required but a useful default)
            ConfDataThresholdContextAttr hostNameContextAttr = new ConfDataThresholdContextAttr();
            hostNameContextAttr.setAttributeType("hostName");
            this.contextAttrs.add(hostNameContextAttr);
        }
    }

    public boolean initQualifyingAttrList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.qualifyingAttrs.clear();

            else {
                List<ConfDataThresholdQualifyingAttr> dbQualifyingAttrs = confDataDAO.selectByColumn("threshold_config_id",this.getId(),
                                                   ConfDataThresholdQualifyingAttr.TYPE_NAME,ConfDataThresholdQualifyingAttr.class);

                setQualifyingAttrList(dbQualifyingAttrs);
            }

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing qualifying attrs for threshold record: " + baseMessage;
            return false;
        }
    }

    public boolean initContextAttrList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.contextAttrs.clear();

            else {
                List<ConfDataThresholdContextAttr> dbContextAttrs = confDataDAO.selectByColumn("threshold_config_id",this.getId(),
                        ConfDataThresholdContextAttr.TYPE_NAME,ConfDataThresholdContextAttr.class);

                setContextAttrList(dbContextAttrs);
            }

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing qualifying attrs for threshold record: " + baseMessage;
            return false;
        }
    }

    public boolean initAlertConfigListChoices(ConfDataDAO confDataDAO) {
        try {
            List<ConfDataAlertingConfig> dbAllAlertingConfigs = confDataDAO.selectAll(ConfDataAlertingConfig.TYPE_NAME,
                    ConfDataAlertingConfig.class);
            this.allAlertingConfigsByLabel.clear();
            this.allAlertingConfigsByLabel.addAll(dbAllAlertingConfigs);
            Collections.sort(this.allAlertingConfigsByLabel, ConfDataObjectByLabelComparator.getInstance());

            this.allAlertingConfigsById.clear();
            this.allAlertingConfigsById.addAll(dbAllAlertingConfigs);
            Collections.sort(this.allAlertingConfigsById, ConfDataObjectByIdComparator.getInstance());

            this.allAlertingConfigNames.clear();
            for(ConfDataAlertingConfig alertingConfig :this.allAlertingConfigsByLabel) {
                this.allAlertingConfigNames.add(alertingConfig.getLabel());
            }
            Collections.sort(this.allAlertingConfigNames);

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing alert configs for threshold record: " + baseMessage;
            return false;
        }
    }

    public String getThresholdName() {
        return this.getLabel();
    }

    public void setThresholdName(String thresholdName) {
        this.setLabel(thresholdName);
    }

    public List<ConfDataThresholdQualifyingAttr> getQualifyingAttrList() {
        return this.qualifyingAttrs;
    }

    private void setQualifyingAttrList(List<ConfDataThresholdQualifyingAttr> attrs) {
        this.qualifyingAttrs.clear();
        this.qualifyingAttrs.addAll(attrs);
    }

    public List<ConfDataThresholdContextAttr> getContextAttrList() {
        return this.contextAttrs;
    }

    private void setContextAttrList(List<ConfDataThresholdContextAttr> attrs) {
        this.contextAttrs.clear();
        this.contextAttrs.addAll(attrs);
    }

    public List<String> getAlertingConfigList() {
        return this.allAlertingConfigNames;
    }

    public String getAlertingConfigName(Long alertConfigId) {

        if(alertConfigId == null)
            return null;

        ConfDataAlertingConfig searchAlertingConfig = new ConfDataAlertingConfig();
        searchAlertingConfig.setId(alertConfigId);

        int index = Collections.binarySearch(this.allAlertingConfigsById, searchAlertingConfig,ConfDataObjectByIdComparator.getInstance());
        if(index >= 0)
            return this.allAlertingConfigsById.get(index).getLabel();
        else
            return null;
    }

    public ConfDataAlertingConfig getAlertingConfigFromAlertingConfigName(String alertConfigName) {

        if(alertConfigName == null)
            return null;

        ConfDataAlertingConfig searchAlertingConfig = new ConfDataAlertingConfig();
        searchAlertingConfig.setLabel(alertConfigName);

        int index = Collections.binarySearch(this.allAlertingConfigsByLabel, searchAlertingConfig,ConfDataObjectByLabelComparator.getInstance());
        if(index >= 0)
            return this.allAlertingConfigsByLabel.get(index);
        else
            return null;
    }

    private boolean validate() {
        // make sure we have at least one of min/max
        if(this.getMinThresholdValue() == null && this.getMaxThresholdValue() == null) {
            lastMessage = "Must specify at least one of Min or Max Threshold Value";
            return false;
        }

        // make sure min is not greater than max
        if(this.getMinThresholdValue() != null && this.getMaxThresholdValue() != null &&
                this.getMinThresholdValue() >= this.getMaxThresholdValue()) {
            lastMessage = "Min Threshold Value cannot be greater than or equal to Max Threshold Value";
            return false;
        }

        // make sure we have a Max Sample Window, if Min Samples > 1
        if(this.getMinThresholdSamples() > 1 && this.getMaxSampleWindowMs() == null) {
            lastMessage = "Must specify a Max Sample Window, if Min Samples > 1";
            return false;
        }

        return true;
    }

    public boolean insert(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            if(!validate()) {
                return false;
            }

            confDataDAO.compoundInsertUpdateDelete(new ThresholdFormInsertIterable<ConfDataObject>(this,this.qualifyingAttrs,this.contextAttrs),null,null);

            lastMessage = "Inserted new threshold record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            //log.warn(ex);

            // TODO: this logic is a hack, need a more elegant way to determine this
            // maybe check against the list before inserting to the db
            String baseMessage = getBaseErrorMessageFromException(ex);
            if(baseMessage.contains("ORA-00001")) {
                baseMessage = "Got uniqueness violation, the threshold name must be unique in the system";
            }
            lastMessage = "Got error inserting threshold record: " + baseMessage;
            return false;
        }
    }

    public boolean update(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            if(!validate()) {
                return false;
            }

            List<ConfDataObject> insertList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> updateList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> deleteList = new ArrayList<ConfDataObject>();

            ConfDataThresholdConfig currDbThreshold = confDataDAO.selectById(this.getId(),this.getTypeName(), ConfDataThresholdConfig.class);
            if(currDbThreshold == null) {
                // this could happen if someone else deletes this object while we're editing...
                insertList.add(this);
            }
            else if(!currDbThreshold.toPropertiesMap().equals(this.toPropertiesMap())) {
                updateList.add(this);
            }


            List<ConfDataThresholdQualifyingAttr> currDbQualifyingAttrs = confDataDAO.selectByColumn("threshold_config_id",this.getId(),
                                                                    ConfDataThresholdQualifyingAttr.TYPE_NAME,
                                                                    ConfDataThresholdQualifyingAttr.class);

            Collections.sort(currDbQualifyingAttrs,ConfDataObjectByIdComparator.getInstance());

            ConfDataThresholdQualifyingAttr searchQualifyingAttr = new ConfDataThresholdQualifyingAttr();

            // loop through edited list, see which are new
            for(ConfDataThresholdQualifyingAttr qualifyingAttr:qualifyingAttrs) {

                Long qualifyingAttrId = qualifyingAttr.getId();
                if(qualifyingAttrId == null) {
                    qualifyingAttr.setLabel(this.getId() + ": " + qualifyingAttr.getAttributeType());
                    qualifyingAttr.setThresholdConfigId(this.getId());
                    insertList.add(qualifyingAttr);
                }
                else {
                    // need to find existing one (if there)
                    searchQualifyingAttr.setId(qualifyingAttrId);
                    int attrIndex = Collections.binarySearch(currDbQualifyingAttrs,searchQualifyingAttr,ConfDataObjectByIdComparator.getInstance());

                    if(attrIndex >= 0) {
                        ConfDataThresholdQualifyingAttr currDbAttr = currDbQualifyingAttrs.get(attrIndex);
                        if(!currDbAttr.toPropertiesMap().equals(qualifyingAttr.toPropertiesMap())) {
                            updateList.add(qualifyingAttr);
                        }
                    }
                    else {
                        qualifyingAttr.setLabel(this.getId() + ": " + qualifyingAttr.getAttributeType());
                        qualifyingAttr.setThresholdConfigId(this.getId());
                        insertList.add(qualifyingAttr);
                    }
                }
            }


            List<ConfDataThresholdContextAttr> currDbContextAttrs = confDataDAO.selectByColumn("threshold_config_id",this.getId(),
                    ConfDataThresholdContextAttr.TYPE_NAME,
                    ConfDataThresholdContextAttr.class);

            Collections.sort(currDbContextAttrs,ConfDataObjectByIdComparator.getInstance());

            ConfDataThresholdContextAttr searchContextAttr = new ConfDataThresholdContextAttr();

            // loop through edited list, see which are new
            for(ConfDataThresholdContextAttr contextAttr:contextAttrs) {
                Long contextAttrId = contextAttr.getId();
                if(contextAttrId == null) {
                    contextAttr.setLabel(this.getId() + ": " + contextAttr.getAttributeType());
                    contextAttr.setThresholdConfigId(this.getId());
                    insertList.add(contextAttr);
                }
                else {
                    // need to find existing one (if still there)
                    searchContextAttr.setId(contextAttrId);
                    int attrIndex = Collections.binarySearch(currDbContextAttrs,searchContextAttr, ConfDataObjectByIdComparator.getInstance());

                    if(attrIndex >= 0) {
                        ConfDataThresholdContextAttr currDbAttr = currDbContextAttrs.get(attrIndex);
                        if(!currDbAttr.toPropertiesMap().equals(contextAttr.toPropertiesMap())) {
                            updateList.add(contextAttr);
                        }
                    }
                    else {
                        contextAttr.setLabel(this.getId() + ": " + contextAttr.getAttributeType());
                        contextAttr.setThresholdConfigId(this.getId());
                        insertList.add(contextAttr);
                    }
                }
            }

            // loop through db lists, see which ones no longer have an entry in the edited list
            for(ConfDataThresholdQualifyingAttr currDbQualifyingAttr:currDbQualifyingAttrs) {
                boolean found = false;
                for(ConfDataThresholdQualifyingAttr qualifyingAttr:qualifyingAttrs) {
                    if(qualifyingAttr.getId() != null && qualifyingAttr.getId().equals(currDbQualifyingAttr.getId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    deleteList.add(currDbQualifyingAttr);
                }
            }

            // loop through db lists, see which ones no longer have an entry in the edited list
            for(ConfDataThresholdContextAttr currDbContextAttr:currDbContextAttrs) {
                boolean found = false;
                for(ConfDataThresholdContextAttr contextAttr:contextAttrs) {
                    if(contextAttr.getId() != null && contextAttr.getId().equals(currDbContextAttr.getId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    deleteList.add(currDbContextAttr);
                }
            }

            confDataDAO.compoundInsertUpdateDelete(insertList,updateList,deleteList);

            lastMessage = "Updated threshold record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error updating threshold record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public boolean delete(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            // all related attrs will be automatically deleted via cascading delete constraints
            confDataDAO.delete(this);

            lastMessage = "Deleted threshold record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error deleting threshold record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public String getLastStatusMessage() {

        return lastMessage;
    }

    private String getBaseErrorMessageFromException(Exception ex) {

        if(ex == null)
            return null;

        Throwable t = ex;
        while(t.getCause() != null) {
            t = t.getCause();
        }

        if (t.getMessage() == null)
            return t.toString();

        return t.getMessage();
    }

    public class ThresholdFormInsertIterable<ConfDataObject> implements Iterable<ConfDataObject> {

        private final ConfDataThresholdConfig threshold;
        private final List<ConfDataThresholdQualifyingAttr> qualifyingAttrs;
        private final List<ConfDataThresholdContextAttr> contextAttrs;

        public ThresholdFormInsertIterable(ConfDataThresholdConfig threshold, List<ConfDataThresholdQualifyingAttr> qualifyingAttrs,
                                                                          List<ConfDataThresholdContextAttr> contextAttrs) {

            this.threshold = threshold;
            this.qualifyingAttrs = qualifyingAttrs;
            this.contextAttrs = contextAttrs;
        }

        public Iterator<ConfDataObject> iterator() {


            return new Iterator<ConfDataObject>() {

                private boolean doneThreshold = false;
                private int currQualifyingAttrIndex = 0;
                private int currContextAttrIndex = 0;

                @Override
                public boolean hasNext() {
                    return (!doneThreshold || currQualifyingAttrIndex < qualifyingAttrs.size() ||
                                                currContextAttrIndex < contextAttrs.size());
                }

                @Override
                public ConfDataObject next() {
                    if (!doneThreshold) {
                        doneThreshold = true;
                        return (ConfDataObject) threshold;
                    }
                    else if (currQualifyingAttrIndex < qualifyingAttrs.size()){
                        ConfDataThresholdQualifyingAttr attr = qualifyingAttrs.get(currQualifyingAttrIndex++);
                        attr.setLabel(threshold.getId() + ": " + attr.getAttributeType() + "->" + attr.getAttributeValue());
                        attr.setThresholdConfigId(threshold.getId());
                        return (ConfDataObject) attr;
                    }
                    else {
                        ConfDataThresholdContextAttr attr = contextAttrs.get(currContextAttrIndex++);
                        attr.setLabel(threshold.getId() + ": " + attr.getAttributeType());
                        attr.setThresholdConfigId(threshold.getId());
                        return (ConfDataObject) attr;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
