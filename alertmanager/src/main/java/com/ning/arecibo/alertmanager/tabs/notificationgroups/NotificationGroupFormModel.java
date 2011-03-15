package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.apache.wicket.IClusterable;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.alertmanager.utils.ConfDataFormModel;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByIdComparator;
import com.ning.arecibo.alertmanager.utils.comparators.NotificationConfigByAddressComparator;

import com.ning.arecibo.util.Logger;


public class NotificationGroupFormModel extends ConfDataNotifGroup implements ConfDataFormModel, IClusterable {
    final static Logger log = Logger.getLogger(NotificationGroupFormModel.class);

    private volatile String lastMessage = null;

    private final List<String> allPersonNickNames;
    private final List<ConfDataPerson> allPersons;
    private final List<ConfDataNotifConfig> allNotificationConfigs;
    private final Map<String,List<ConfDataNotifConfig>> notificationConfigsByPersonNickName;
    private final List<ConfDataNotifConfig> notificationConfigs;

    public NotificationGroupFormModel() {
        this(null);
    }

    public NotificationGroupFormModel(ConfDataNotifGroup confDataNotifGroup) {

        if(confDataNotifGroup != null)
            this.setPropertiesFromMap(confDataNotifGroup.toPropertiesMap());

        this.allPersonNickNames = new ArrayList<String>();
        this.allPersons = new ArrayList<ConfDataPerson>();
        this.allNotificationConfigs = new ArrayList<ConfDataNotifConfig>();
        this.notificationConfigsByPersonNickName = new HashMap<String,List<ConfDataNotifConfig>>();
        this.notificationConfigs = new ArrayList<ConfDataNotifConfig>();
    }

    public boolean initNotificationConfigList(ConfDataDAO confDataDAO) {
        try {
            if(this.getId() == null)
                this.notificationConfigs.clear();

            else {
                List<ConfDataNotifConfig> configList = new ArrayList<ConfDataNotifConfig>();

                List<ConfDataNotifMapping> currDbNotificationMappings = confDataDAO.selectByColumn("notif_group_id",this.getId(),
                        ConfDataNotifMapping.TYPE_NAME, ConfDataNotifMapping.class);

                for(ConfDataNotifMapping mapping:currDbNotificationMappings) {
                    ConfDataNotifConfig config = confDataDAO.selectById(mapping.getNotifConfigId(),
                            ConfDataNotifConfig.TYPE_NAME, ConfDataNotifConfig.class);

                    configList.add(config);
                }

                setNotificationConfigList(configList);
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

    public boolean initNotificationConfigListChoices(ConfDataDAO confDataDAO) {
        try {
            List<ConfDataNotifConfig> allNotificationConfigs = confDataDAO.selectAll(ConfDataNotifConfig.TYPE_NAME,
                    ConfDataNotifConfig.class);
            this.allNotificationConfigs.clear();
            this.allNotificationConfigs.addAll(allNotificationConfigs);
            Collections.sort(this.allNotificationConfigs, NotificationConfigByAddressComparator.getInstance());

            List<ConfDataPerson> allPersons = confDataDAO.selectAll(ConfDataPerson.TYPE_NAME,
                    ConfDataPerson.class);
            this.allPersons.clear();
            this.allPersons.addAll(allPersons);
            Collections.sort(this.allPersons, ConfDataObjectByIdComparator.getInstance());

            // create per person notification list
            this.notificationConfigsByPersonNickName.clear();
            this.allPersonNickNames.clear();

            ConfDataPerson searchPerson = new ConfDataPerson();
            for(ConfDataNotifConfig config:allNotificationConfigs) {
                // get person
                searchPerson.setId(config.getPersonId());
                int personIndex = Collections.binarySearch(this.allPersons,searchPerson, ConfDataObjectByIdComparator.getInstance());
                if(personIndex < 0)
                    continue;

                ConfDataPerson person = this.allPersons.get(personIndex);

                List<ConfDataNotifConfig> perPersonConfigs = this.notificationConfigsByPersonNickName.get(person.getLabel());
                if(perPersonConfigs == null) {
                    perPersonConfigs = new ArrayList<ConfDataNotifConfig>();
                    this.notificationConfigsByPersonNickName.put(person.getLabel(),perPersonConfigs);
                }
                perPersonConfigs.add(config);

                if(!this.allPersonNickNames.contains(person.getLabel()))
                    this.allPersonNickNames.add(person.getLabel());
            }

            Collections.sort(this.allPersonNickNames);

            return true;
        }
        catch(Exception ex) {
            log.warn(ex);

            String baseMessage = getBaseErrorMessageFromException(ex);
            lastMessage = "failed to load existing notificationconfigs for person record: " + baseMessage;
            return false;
        }
    }

    public String getGroupName() {
        return this.getLabel();
    }

    public void setGroupName(String groupName) {
        this.setLabel(groupName);
    }

    public List<ConfDataNotifConfig> getNotificationConfigList() {
        return this.notificationConfigs;
    }

    private void setNotificationConfigList(List<ConfDataNotifConfig> configs) {
        this.notificationConfigs.clear();
        this.notificationConfigs.addAll(configs);
    }

    public List<String> getPersonList() {
        return this.allPersonNickNames;
    }

    public String getPersonNickName(Long personId) {

        if(personId == null)
            return null;

        ConfDataPerson searchPerson = new ConfDataPerson();
        searchPerson.setId(personId);

        int index = Collections.binarySearch(this.allPersons,searchPerson,ConfDataObjectByIdComparator.getInstance());
        if(index >= 0)
            return this.allPersons.get(index).getLabel();
        else
            return null;
    }

    public ConfDataNotifConfig getNotificationConfigByAddress(String address) {
        ConfDataNotifConfig searchConfig = new ConfDataNotifConfig();
        searchConfig.setAddress(address);

        int foundIndex = Collections.binarySearch(this.allNotificationConfigs,searchConfig, NotificationConfigByAddressComparator.getInstance());

        if(foundIndex >= 0)
            return this.allNotificationConfigs.get(foundIndex);
        else
            return null;
    }

    public List<String> getNotificationConfigAddressList(String personNickName) {
        ArrayList<String> addressList = new ArrayList<String>();
        if(personNickName == null)
            return addressList;

        List<ConfDataNotifConfig> configs = this.notificationConfigsByPersonNickName.get(personNickName);

        if(configs != null) {
            for(ConfDataNotifConfig config:configs) {
                addressList.add(config.getAddress());
            }
        }

        return addressList;
    }

    public List<String> getNotificationConfigAddressList(Long personId) {
        return getNotificationConfigAddressList(getPersonNickName(personId));
    }

    public boolean insert(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            confDataDAO.compoundInsertUpdateDelete(new NotifGroupFormInsertIterable<ConfDataObject>(this,this.notificationConfigs),null,null);

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

            ConfDataNotifGroup currDbNotifGroup = confDataDAO.selectById(this.getId(),this.getTypeName(), ConfDataNotifGroup.class);
            if(currDbNotifGroup == null) {
                // this could happen if someone else deletes this person while we're editing...
                insertList.add(this);
            }
            else if(!currDbNotifGroup.toPropertiesMap().equals(this.toPropertiesMap())) {
                updateList.add(this);
            }

            List<ConfDataNotifMapping> currDbNotifMappings = confDataDAO.selectByColumn("notif_group_id",this.getId(),
                                                                    ConfDataNotifMapping.TYPE_NAME,
                                                                    ConfDataNotifMapping.class);

            // loop through edited notification list, see which are new
            //TODO: Consider sorting/binary searching, instead of nested for loops
            for(ConfDataNotifConfig notificationConfig:notificationConfigs) {
                Long notifConfigId = notificationConfig.getId();

                // need to find existing one (if still there)
                boolean found = false;
                for(ConfDataNotifMapping currDbNotifMapping:currDbNotifMappings) {
                    if(currDbNotifMapping.getNotifConfigId().equals(notifConfigId)) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // this needs to be inserted (could have had it's personId set after previous failed insert, etc.)
                    ConfDataNotifMapping mapping = new ConfDataNotifMapping();
                    initNotificationMapping(mapping,this,notificationConfig);
                    insertList.add(mapping);
                }
            }

            // loop through db list, see which ones no longer have an entry in the edited list
            //TODO: Consider sorting/binary searching, instead of nested for loops
            for(ConfDataNotifMapping currDbNotifMapping:currDbNotifMappings) {
                boolean found = false;
                for(ConfDataNotifConfig notificationConfig:notificationConfigs) {
                    if(notificationConfig.getId() != null && notificationConfig.getId().equals(currDbNotifMapping.getNotifConfigId())) {
                        found = true;
                        break;
                    }
                }

                if(!found) {
                    // delete this notification from the db
                    deleteList.add(currDbNotifMapping);
                }
            }

            confDataDAO.compoundInsertUpdateDelete(insertList,updateList,deleteList);

            lastMessage = "Updated notification group record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error updating notification group record: " + getBaseErrorMessageFromException(ex);
            return false;
        }
    }

    public boolean delete(ConfDataDAO confDataDAO) {

        try {
            lastMessage = null;

            // all related notification configs will be automatically deleted via cascading delete constraints
            confDataDAO.delete(this);

            lastMessage = "Deleted notification group record: '" + this.getLabel() + "'";

            return true;
        }
        catch(Exception ex) {
            lastMessage = "Got error deleting notification group record: " + getBaseErrorMessageFromException(ex);
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

    private void initNotificationMapping(ConfDataNotifMapping mapping, ConfDataNotifGroup group, ConfDataNotifConfig config) {
        mapping.setNotifGroupId(group.getId());
        mapping.setNotifConfigId(config.getId());
        mapping.setLabel("mapping_" + group.getId() + "_to_" + config.getId());
    }

    public class NotifGroupFormInsertIterable<ConfDataObject> implements Iterable<ConfDataObject> {

        private final ConfDataNotifGroup notifGroup;
        private final List<ConfDataNotifConfig> notificationConfigs;

        public NotifGroupFormInsertIterable(ConfDataNotifGroup notifGroup, List<ConfDataNotifConfig> notificationConfigs) {

            this.notifGroup = notifGroup;
            this.notificationConfigs = notificationConfigs;
        }

        public Iterator<ConfDataObject> iterator() {


            return new Iterator<ConfDataObject>() {

                private boolean doneNotifGroup = false;
                private int currNotificationConfigIndex = 0;

                @Override
                public boolean hasNext() {
                    return (!doneNotifGroup || currNotificationConfigIndex < notificationConfigs.size());
                }

                @Override
                public ConfDataObject next() {
                    if (!doneNotifGroup) {
                        doneNotifGroup = true;
                        return (ConfDataObject) notifGroup;
                    }
                    else {
                        ConfDataNotifConfig config = notificationConfigs.get(currNotificationConfigIndex++);

                        ConfDataNotifMapping mapping = new ConfDataNotifMapping();
                        initNotificationMapping(mapping,notifGroup,config);

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
