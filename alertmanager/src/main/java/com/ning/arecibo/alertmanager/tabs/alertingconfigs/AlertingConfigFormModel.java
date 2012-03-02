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

package com.ning.arecibo.alertmanager.tabs.alertingconfigs;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import org.apache.wicket.IClusterable;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.enums.NotificationRepeatMode;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKeyMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroupMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alertmanager.utils.ConfDataFormModel;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByLabelComparator;

import com.ning.arecibo.util.Logger;


public class AlertingConfigFormModel extends ConfDataAlertingConfig implements ConfDataFormModel, IClusterable {
    final static Logger log = Logger.getLogger(AlertingConfigFormModel.class);

    private volatile String lastMessage = null;

    private final List<String> allGroupNames;
    private final List<ConfDataNotifGroup> allGroups;
    private final List<ConfDataNotifGroup> notificationGroups;
    private final List<String> allManagingKeyNames;
    private final List<ConfDataManagingKey> allManagingKeys;
    private final List<ConfDataManagingKey> managingKeys;

    public AlertingConfigFormModel() {
        this(null);
    }

    public AlertingConfigFormModel(ConfDataAlertingConfig confDataAlertingConfig) {

        if(confDataAlertingConfig != null) {
            this.setPropertiesFromMap(confDataAlertingConfig.toPropertiesMap());
        }
        else {
            // some defaults
            this.setNotifRepeatMode(NotificationRepeatMode.NO_REPEAT);
            this.setNotifOnRecovery(true);
        }

        this.allGroupNames = new ArrayList<String>();
        this.allGroups = new ArrayList<ConfDataNotifGroup>();
        this.notificationGroups = new ArrayList<ConfDataNotifGroup>();

        this.allManagingKeyNames = new ArrayList<String>();
        this.allManagingKeys = new ArrayList<ConfDataManagingKey>();
        this.managingKeys = new ArrayList<ConfDataManagingKey>();
    }

    public boolean initNotificationGroupList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.notificationGroups.clear();

            else {
                List<ConfDataNotifGroup> groupList = new ArrayList<ConfDataNotifGroup>();

                List<ConfDataNotifGroupMapping> currDbNotifGroupMappings = confDataDAO.selectByColumn("alerting_config_id",this.getId(),
                        ConfDataNotifGroupMapping.TYPE_NAME, ConfDataNotifGroupMapping.class);

                for(ConfDataNotifGroupMapping mapping:currDbNotifGroupMappings) {
                    ConfDataNotifGroup group = confDataDAO.selectById(mapping.getNotifGroupId(),
                            ConfDataNotifGroup.TYPE_NAME, ConfDataNotifGroup.class);

                    groupList.add(group);
                }

                setNotificationGroupList(groupList);
            }

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing notification groups for alerting config record: " + baseMessage;
            return false;
        }
    }

    public boolean initManagingKeyList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.managingKeys.clear();

            else {
                List<ConfDataManagingKey> managingKeyList = new ArrayList<ConfDataManagingKey>();

                List<ConfDataManagingKeyMapping> managingKeyMappings = confDataDAO.selectByColumn("alerting_config_id",this.getId(),
                        ConfDataManagingKeyMapping.TYPE_NAME, ConfDataManagingKeyMapping.class);

                for(ConfDataManagingKeyMapping mapping:managingKeyMappings) {
                    ConfDataManagingKey managingKey = confDataDAO.selectById(mapping.getManagingKeyId(),
                            ConfDataManagingKey.TYPE_NAME, ConfDataManagingKey.class);

                    managingKeyList.add(managingKey);
                }

                setManagingKeyList(managingKeyList);
            }

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing notification groups for alerting config record: " + baseMessage;
            return false;
        }
    }

    public boolean initNotificationGroupListChoices(ConfDataDAO confDataDAO) {
        try {
            List<ConfDataNotifGroup> allNotificationGroups = confDataDAO.selectAll(ConfDataNotifGroup.TYPE_NAME,
                    ConfDataNotifGroup.class);
            this.allGroups.clear();
            this.allGroups.addAll(allNotificationGroups);
            Collections.sort(this.allGroups,ConfDataObjectByLabelComparator.getInstance());

            this.allGroupNames.clear();

            for(ConfDataNotifGroup group:allNotificationGroups) {
                this.allGroupNames.add(group.getLabel());
            }
            Collections.sort(this.allGroupNames);

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing notification groups for alerting config record: " + baseMessage;
            return false;
        }
    }

    public boolean initManagingKeyListChoices(ConfDataDAO confDataDAO) {
        try {
            List<ConfDataManagingKey> allManagingKeys = confDataDAO.selectAll(ConfDataManagingKey.TYPE_NAME,
                    ConfDataManagingKey.class);
            this.allManagingKeys.clear();
            this.allManagingKeys.addAll(allManagingKeys);
            Collections.sort(this.allManagingKeys,ConfDataObjectByLabelComparator.getInstance());

            this.allManagingKeyNames.clear();

            for(ConfDataManagingKey managingKey:this.allManagingKeys) {
                this.allManagingKeyNames.add(managingKey.getKey());
            }
            Collections.sort(this.allManagingKeyNames);

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing managing keys names for alerting config record: " + baseMessage;
            return false;
        }
    }

    public ConfDataNotifGroup getNotificationGroupByGroupName(String groupName) {
        ConfDataNotifGroup searchGroup = new ConfDataNotifGroup();
        searchGroup.setLabel(groupName);

        int groupIndex = Collections.binarySearch(this.allGroups,searchGroup,ConfDataObjectByLabelComparator.getInstance());
        if(groupIndex >= 0)
            return this.allGroups.get(groupIndex);
        else
            return null;
    }

    public ConfDataManagingKey getManagingKeyByManagingKeyName(String keyName) {
        ConfDataManagingKey searchKey = new ConfDataManagingKey();
        searchKey.setLabel(keyName);

        int keyIndex = Collections.binarySearch(this.allManagingKeys,searchKey, ConfDataObjectByLabelComparator.getInstance());
        if(keyIndex >= 0)
            return this.allManagingKeys.get(keyIndex);
        else
            return null;
    }

    public String getAlertingConfigName() {
        return this.getLabel();
    }

    public void setAlertingConfigName(String alertingConfigName) {
        this.setLabel(alertingConfigName);
    }

    public List<ConfDataNotifGroup> getNotificationGroupList() {
        return this.notificationGroups;
    }

    private void setNotificationGroupList(List<ConfDataNotifGroup> groups) {
        this.notificationGroups.clear();
        this.notificationGroups.addAll(groups);
    }

    public List<ConfDataManagingKey> getManagingKeyList() {
        return this.managingKeys;
    }

    private void setManagingKeyList(List<ConfDataManagingKey> managingKeys) {
        this.managingKeys.clear();
        this.managingKeys.addAll(managingKeys);
    }

    public List<String> getNotificationGroupNameList() {
        return this.allGroupNames;
    }

    public List<String> getManagingKeyNameList() {
        return this.allManagingKeyNames;
    }

    public boolean insert(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            confDataDAO.compoundInsertUpdateDelete(new AlertConfigFormInsertIterable<ConfDataObject>(this,this.notificationGroups,this.managingKeys),null,null);

            lastMessage = "Inserted new notification group record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            //log.warn(ex);

            // TODO: this logic is a hack, need a more elegant way to determine this
            // maybe check against the list before inserting to the db
            String baseMessage = getBaseErrorMessageFromException(ex);
            if(baseMessage.contains("ORA-00001")) {
                baseMessage = "Got uniqueness violation, the notification group nick name must be unique in the system";
            }
            lastMessage = "Got error inserting notification group record: " + baseMessage;
            return false;
        }
    }

    public boolean update(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            // get current db versions, to compare which of the notification configs are updates, inserts or deletes
            List<ConfDataObject> insertList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> updateList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> deleteList = new ArrayList<ConfDataObject>();

            ConfDataAlertingConfig currDbAlertingConfig = confDataDAO.selectById(this.getId(),this.getTypeName(), ConfDataAlertingConfig.class);
            if(currDbAlertingConfig == null) {
                // this could happen if someone else deletes this object while we're editing...
                insertList.add(this);
            }
            else if(!currDbAlertingConfig.toPropertiesMap().equals(this.toPropertiesMap())) {
                updateList.add(this);
            }

            // check notification group mappings
            List<ConfDataNotifGroupMapping> currDbNotifGroupMappings = confDataDAO.selectByColumn("alerting_config_id",this.getId(),
                                                                    ConfDataNotifGroupMapping.TYPE_NAME,
                                                                    ConfDataNotifGroupMapping.class);

            // loop through edited group list, see which are new
            for(ConfDataNotifGroup notificationGroup:notificationGroups) {
                Long notifGroupId = notificationGroup.getId();

                // need to find existing one (if still there)
                boolean found = false;
                for(ConfDataNotifGroupMapping currDbNotifGroupMapping:currDbNotifGroupMappings) {
                    if(currDbNotifGroupMapping.getNotifGroupId().equals(notifGroupId)) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // this needs to be inserted (could have had it's id set after previous failed insert, etc.)
                    ConfDataNotifGroupMapping mapping = new ConfDataNotifGroupMapping();
                    initNotificationMapping(mapping,this,notificationGroup);
                    insertList.add(mapping);
                }
            }

            // loop through db list, see which ones no longer have an entry in the edited list
            for(ConfDataNotifGroupMapping currDbNotifGroupMapping:currDbNotifGroupMappings) {
                boolean found = false;
                for(ConfDataNotifGroup notificationGroup:notificationGroups) {
                    if(notificationGroup.getId() != null && notificationGroup.getId().equals(currDbNotifGroupMapping.getNotifGroupId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // delete this notification from the db
                    deleteList.add(currDbNotifGroupMapping);
                }
            }


            // check managing key mappings
            List<ConfDataManagingKeyMapping> currDbManagingKeyMappings = confDataDAO.selectByColumn("alerting_config_id",this.getId(),
                    ConfDataManagingKeyMapping.TYPE_NAME,
                    ConfDataManagingKeyMapping.class);

            // loop through edited group list, see which are new
            for(ConfDataManagingKey managingKey:managingKeys) {
                Long notifGroupId = managingKey.getId();

                // need to find existing one (if still there)
                boolean found = false;
                for(ConfDataManagingKeyMapping currDbManagingKeyMapping:currDbManagingKeyMappings) {
                    if(currDbManagingKeyMapping.getManagingKeyId().equals(notifGroupId)) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // this needs to be inserted (could have had it's id set after previous failed insert, etc.)
                    ConfDataManagingKeyMapping mapping = new ConfDataManagingKeyMapping();
                    initManagingKeyMapping(mapping,this,managingKey);
                    insertList.add(mapping);
                }
            }

            // loop through db list, see which ones no longer have an entry in the edited list
            for(ConfDataManagingKeyMapping currDbManagingKeyMapping:currDbManagingKeyMappings) {
                boolean found = false;
                for(ConfDataManagingKey managingKey:managingKeys) {
                    if(managingKey.getId() != null && managingKey.getId().equals(currDbManagingKeyMapping.getManagingKeyId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // delete this notification from the db
                    deleteList.add(currDbManagingKeyMapping);
                }
            }



            // do the update
            confDataDAO.compoundInsertUpdateDelete(insertList,updateList,deleteList);

            lastMessage = "Updated alert config record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error updating alert config record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public boolean delete(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            confDataDAO.delete(this);

            lastMessage = "Deleted alert config record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error deleting alert config record: " + getBaseErrorMessageFromException(ex);
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
            log.warn(ex);
            t = t.getCause();
        }

        if(t.getMessage() == null)
            return t.toString();

        return t.getMessage();
    }

    private void initNotificationMapping(ConfDataNotifGroupMapping mapping, ConfDataAlertingConfig alertingConfig, ConfDataNotifGroup group) {
        mapping.setNotifGroupId(group.getId());
        mapping.setAlertingConfigId(alertingConfig.getId());
        mapping.setLabel("mapping_" + alertingConfig.getId() + "_to_" + group.getId());
    }

    private void initManagingKeyMapping(ConfDataManagingKeyMapping mapping, ConfDataAlertingConfig alertingConfig, ConfDataManagingKey managingKey) {
        mapping.setManagingKeyId(managingKey.getId());
        mapping.setAlertingConfigId(alertingConfig.getId());
        mapping.setLabel("mapping_" + alertingConfig.getId() + "_to_" + managingKey.getId());
    }

    public class AlertConfigFormInsertIterable<ConfDataObject> implements Iterable<ConfDataObject> {

        private final ConfDataAlertingConfig alertingConfig;
        private final List<ConfDataNotifGroup> notificationGroups;
        private final List<ConfDataManagingKey> managingKeys;

        public AlertConfigFormInsertIterable(ConfDataAlertingConfig alertingConfig,
                                             List<ConfDataNotifGroup> notificationGroups,
                                             List<ConfDataManagingKey> managingKeys) {

            this.alertingConfig = alertingConfig;
            this.notificationGroups = notificationGroups;
            this.managingKeys = managingKeys;
        }

        public Iterator<ConfDataObject> iterator() {


            return new Iterator<ConfDataObject>() {

                private boolean doneAlertConfig = false;
                private int currNotificationGroupIndex = 0;
                private int currManagingKeyIndex = 0;

                @Override
                public boolean hasNext() {
                    return (!doneAlertConfig ||
                            currNotificationGroupIndex < notificationGroups.size() ||
                            currManagingKeyIndex < managingKeys.size());
                }

                @Override
                public ConfDataObject next() {
                    if (!doneAlertConfig) {
                        doneAlertConfig = true;
                        return (ConfDataObject) alertingConfig;
                    }
                    else if(currNotificationGroupIndex < notificationGroups.size()){
                        ConfDataNotifGroup group = notificationGroups.get(currNotificationGroupIndex++);

                        ConfDataNotifGroupMapping mapping = new ConfDataNotifGroupMapping();
                        initNotificationMapping(mapping,alertingConfig,group);

                        return (ConfDataObject) mapping;
                    }
                    else {
                        ConfDataManagingKey managingKey = managingKeys.get(currManagingKeyIndex++);

                        ConfDataManagingKeyMapping mapping = new ConfDataManagingKeyMapping();
                        initManagingKeyMapping(mapping,alertingConfig,managingKey);

                        return (ConfDataObject) mapping;
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
