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

package com.ning.arecibo.alertmanager.tabs.people;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.wicket.IClusterable;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.alertmanager.utils.ConfDataFormModel;

import com.ning.arecibo.util.Logger;


public class PersonFormModel extends ConfDataPerson implements ConfDataFormModel, IClusterable {
    final static Logger log = Logger.getLogger(PersonFormModel.class);

    private volatile String lastMessage = null;

    private final List<ConfDataNotifConfig> notificationConfigs;

    public PersonFormModel() {
        this(null);
    }

    public PersonFormModel(ConfDataPerson confDataPerson) {
        this(confDataPerson,null);
    }

    public PersonFormModel(ConfDataPerson confDataPerson, List<ConfDataNotifConfig> notificationConfigs) {

        if(confDataPerson != null)
            this.populatePropertiesFromMap(confDataPerson.toPropertiesMap());

        if(notificationConfigs == null) {
            notificationConfigs = new ArrayList<ConfDataNotifConfig>();
        }

        this.notificationConfigs = notificationConfigs;

        if(this.isGroupAlias == null) {
            this.isGroupAlias = false;
        }
    }

    public String getNickName() {
        return this.getLabel();
    }

    public void setNickName(String nickName) {
        this.setLabel(nickName);
    }

    public List<ConfDataNotifConfig> getNotificationConfigList() {
        return this.notificationConfigs;
    }

    public void setNotificationConfigList(List<ConfDataNotifConfig> configs) {
        this.notificationConfigs.clear();
        this.notificationConfigs.addAll(configs);
    }

    public boolean initNotificationConfigList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.notificationConfigs.clear();

            else {
                List<ConfDataNotifConfig> currDbNotificationConfigs = confDataDAO.selectByColumn("person_id",this.getId(),
                                                            ConfDataNotifConfig.TYPE_NAME, ConfDataNotifConfig.class);
                setNotificationConfigList(currDbNotificationConfigs);
            }

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing notificationconfigs for person record: " + baseMessage;
            return false;
        }
    }

    public boolean insert(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            if(this.isGroupAlias) {
                this.firstName = null;
                this.lastName = null;
            }

            confDataDAO.compoundInsertUpdateDelete(new PersonFormInsertIterable<ConfDataObject>(this,this.notificationConfigs),null,null);

            if(this.isGroupAlias) {
                lastMessage = "Inserted new person (alias) record: '" + this.getLabel() + "'";
            }
            else {
                lastMessage = "Inserted new person record: '" + this.getLabel() + "'";
            }

            return true;
        }
        catch(Exception ex) {
            //log.warn(ex);

            // TODO: this logic is a hack, need a more elegant way to determine this
            // maybe check against the list before inserting to the db
            String baseMessage = getBaseErrorMessageFromException(ex);
            if(baseMessage.contains("ORA-00001")) {
                baseMessage = "Got uniqueness violation, the person nick name, and all email addresses, must be unique in the system";
            }
            lastMessage = "Got error inserting person record: " + baseMessage;
            return false;
        }
    }

    public boolean update(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            if(this.isGroupAlias) {
                this.firstName = null;
                this.lastName = null;
            }


            // get current db versions, to compare which of the notification configs are updates, inserts or deletes
            List<ConfDataObject> insertList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> updateList = new ArrayList<ConfDataObject>();
            List<ConfDataObject> deleteList = new ArrayList<ConfDataObject>();

            ConfDataPerson currDbPerson = confDataDAO.selectById(this.getId(),this.getTypeName(),ConfDataPerson.class);
            if(currDbPerson == null) {
                // this could happen if someone else deletes this person while we're editing...
                insertList.add(this);
            }
            else if(!currDbPerson.toPropertiesMap().equals(this.toPropertiesMap())) {
                updateList.add(this);
            }

            List<ConfDataNotifConfig> currDbNotificationConfigs = confDataDAO.selectByColumn("person_id",this.getId(),
                                                                    ConfDataNotifConfig.TYPE_NAME,
                                                                    ConfDataNotifConfig.class);

            // loop through edited notification list, see which are new, and which have changed
            //TODO: Consider sorting/binary searching, instead of nested for loops
            for(ConfDataNotifConfig notificationConfig:notificationConfigs) {
                Long personId = notificationConfig.getPersonId();

                if(notificationConfig.getId() == null) {
                    // this is definitely a newly created one
                    initNotificationConfig(notificationConfig,this);
                    insertList.add(notificationConfig);
                }
                else {
                    // need to find existing one (if still there)
                    boolean found = false;
                    for(ConfDataNotifConfig currDbNotificationConfig:currDbNotificationConfigs) {
                        if(currDbNotificationConfig.getId().equals(notificationConfig.getId())) {
                            if(!currDbNotificationConfig.toPropertiesMap().equals(notificationConfig.toPropertiesMap())) {
                                updateList.add(notificationConfig);
                            }
                            found = true;
                            break;
                        }
                    }

                    if(!found) {
                        // this needs to be inserted (could have had it's personId set after previous failed insert, etc.)
                        initNotificationConfig(notificationConfig,this);
                        insertList.add(notificationConfig);
                    }
                }
            }

            // loop through db list, see which ones no longer have an entry in the edited list
            //TODO: Consider sorting/binary searching, instead of nested for loops
            for(ConfDataNotifConfig currDbNotificationConfig:currDbNotificationConfigs) {
                boolean found = false;
                for(ConfDataNotifConfig notificationConfig:notificationConfigs) {
                    if(notificationConfig.getId() != null && notificationConfig.getId().equals(currDbNotificationConfig.getId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // delete this notification from the db
                    deleteList.add(currDbNotificationConfig);
                }
            }

            confDataDAO.compoundInsertUpdateDelete(insertList,updateList,deleteList);

            if(this.isGroupAlias)
                lastMessage = "Updated person (alias) record: '" + this.getLabel() + "'";
            else
                lastMessage = "Updated person record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error updating person record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public boolean delete(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            // all related notification configs will be automatically deleted via cascading delete constraints
            confDataDAO.delete(this);

            if(this.isGroupAlias)
                lastMessage = "Deleted person (alias) record: '" + this.getLabel() + "'";
            else
                lastMessage = "Deleted person record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error deleting person record: " + getBaseErrorMessageFromException(ex);
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

    public void dumpNotificationConfigs() {
        if(!log.isDebugEnabled())
            return;

        int count=0;
        log.debug("Dumping notificationConfigs:");
        for(ConfDataNotifConfig config:notificationConfigs) {
            log.debug("\t[%d]: %s",count++,config.getAddress());
        }
    }

    private void initNotificationConfig(ConfDataNotifConfig notificationConfig,ConfDataPerson person) {
        notificationConfig.setPersonId(person.getId());

        // set label
        //TODO:  for now just use a truncated version of the email address, need to devise something better
        //          this can fail to distinguish between emails which differ after the 32nd character, etc...
        String label = notificationConfig.getAddress();
        if (label.length() > 32)
            label = label.substring(0, 31);
        notificationConfig.setLabel(label);

    }

    public class PersonFormInsertIterable<ConfDataObject> implements Iterable<ConfDataObject> {

        private final ConfDataPerson person;
        private final List<ConfDataNotifConfig> notificationConfigs;

        public PersonFormInsertIterable(ConfDataPerson person, List<ConfDataNotifConfig> notificationConfigs) {

            this.person = person;
            this.notificationConfigs = notificationConfigs;
        }

        public Iterator<ConfDataObject> iterator() {


            return new Iterator<ConfDataObject>() {

                private boolean donePerson = false;
                private int currNotificationConfigIndex = 0;

                @Override
                public boolean hasNext() {
                    return (!donePerson || currNotificationConfigIndex < notificationConfigs.size());
                }

                @Override
                public ConfDataObject next() {
                    if (!donePerson) {
                        donePerson = true;
                        return (ConfDataObject) person;
                    }
                    else {
                        ConfDataNotifConfig notificationConfig = notificationConfigs.get(currNotificationConfigIndex++);
                        initNotificationConfig(notificationConfig,person);

                        return (ConfDataObject) notificationConfig;
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
