package com.ning.arecibo.alertmanager.tabs.alertingconfigs;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.WildcardListModel;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.Session;
import org.apache.wicket.Page;
import org.apache.wicket.Component;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.enums.NotificationRepeatMode;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKeyMapping;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroupMapping;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByIdComparator;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByLabelComparator;

import com.ning.arecibo.util.Logger;



import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class AlertingConfigsPanel extends Panel {
    final static Logger log = Logger.getLogger(AlertingConfigsPanel.class);

    final FeedbackPanel feedback;
    final SortableConfDataProvider<AlertingConfigExtendedForDisplay> dataProvider;
    final Map<Long,List<ConfDataNotifGroup>> notificationGroups;
    final Map<Long,List<ConfDataManagingKey>> managingKeys;
    final List<IColumn<AlertingConfigExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<AlertingConfigExtendedForDisplay> dataTable;
    final ModalWindow newAlertConfigModalWindow;
    final ModalWindowWithAlertingConfig editAlertConfigModalWindow;
    final ModalWindowWithAlertingConfig deleteAlertConfigModalWindow;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public AlertingConfigsPanel(String id) {
        super(id);

        
        final WebMarkupContainerWithAssociatedMarkup explainPanel1 = new WebMarkupContainerWithAssociatedMarkup("explanation1");
        explainPanel1.setVisible(false);
        explainPanel1.setOutputMarkupPlaceholderTag(true);
        add(explainPanel1);

        final WebMarkupContainerWithAssociatedMarkup explainPanel2 = new WebMarkupContainerWithAssociatedMarkup("explanation2");
        explainPanel2.setVisible(false);
        explainPanel2.setOutputMarkupPlaceholderTag(true);
        add(explainPanel2);

        final WebMarkupContainerWithAssociatedMarkup explainPanel3 = new WebMarkupContainerWithAssociatedMarkup("explanation3");
        explainPanel3.setVisible(false);
        explainPanel3.setOutputMarkupPlaceholderTag(true);
        add(explainPanel3);

        final AjaxLink explainLink1 = new AjaxLink("explainLink1") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel1.setVisible(!explainPanel1.isVisible());
                explainPanel2.setVisible(false);
                explainPanel3.setVisible(false);
                target.addComponent(explainPanel1);
                target.addComponent(explainPanel2);
                target.addComponent(explainPanel3);
            }
        };
        add(explainLink1);

        final AjaxLink explainLink2 = new AjaxLink("explainLink2") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel2.setVisible(!explainPanel2.isVisible());
                explainPanel1.setVisible(false);
                explainPanel3.setVisible(false);
                target.addComponent(explainPanel1);
                target.addComponent(explainPanel2);
                target.addComponent(explainPanel3);
            }
        };
        add(explainLink2);

        final AjaxLink explainLink3 = new AjaxLink("explainLink3") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel3.setVisible(!explainPanel3.isVisible());
                explainPanel1.setVisible(false);
                explainPanel2.setVisible(false);
                target.addComponent(explainPanel1);
                target.addComponent(explainPanel2);
                target.addComponent(explainPanel3);
            }
        };
        add(explainLink3);


        notificationGroups = getNotificationGroupTableData();
        managingKeys = getManagingKeyTableData();
        List<AlertingConfigExtendedForDisplay> alertingConfigs = getAlertingConfigTableData(notificationGroups,managingKeys);

        dataProvider = new SortableConfDataProvider<AlertingConfigExtendedForDisplay>(alertingConfigs,"label",AlertingConfigExtendedForDisplay.class);

        columns = new ArrayList<IColumn<AlertingConfigExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertingConfigExtendedForDisplay>(new Model<String>("Alerting Config Name"), "label", "label", "columnWidth35"));

        columns.add(new ChoiceFilteredPropertyColumn<AlertingConfigExtendedForDisplay, NotificationRepeatMode>(new Model<String>("Repeat Mode"), "notifRepeatMode", "notifRepeatMode",
                new WildcardListModel<NotificationRepeatMode>(Arrays.asList((NotificationRepeatMode) null, NotificationRepeatMode.NO_REPEAT, NotificationRepeatMode.UNTIL_CLEARED))));

        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertingConfigExtendedForDisplay>(new Model<String>("Repeat Interval"), "notifRepeatIntervalMs", "notifRepeatIntervalMs", "columnWidth20"));

        columns.add(new ChoiceFilteredPropertyColumn<AlertingConfigExtendedForDisplay, Boolean>(new Model<String>("Notify On Recovery?"), "notifOnRecovery", "notifOnRecovery",
                new WildcardListModel<Boolean>(Arrays.asList((Boolean) null, Boolean.TRUE, Boolean.FALSE))));

        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertingConfigExtendedForDisplay>(new Model<String>("Notification Groups"),"extendedNotificationGroups","extendedNotificationGroups","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<AlertingConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<AlertingConfigExtendedForDisplay> model) {
                cellItem.add(new NotificationGroupsPanel(componentId, model));
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertingConfigExtendedForDisplay>(new Model<String>("Managing Rules"),"extendedManagingKeys","extendedManagingKeys","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<AlertingConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<AlertingConfigExtendedForDisplay> model) {
                cellItem.add(new ManagingKeysPanel(componentId, model));
            }
        });

        columns.add(new ChoiceFilteredPropertyColumn<AlertingConfigExtendedForDisplay, Boolean>(new Model<String>("Is Enabled"), "enabled", "enabled",
                new WildcardListModel<Boolean>(Arrays.asList((Boolean) null, Boolean.TRUE, Boolean.FALSE))));
        
        columns.add(new TimestampPropertyColumn<AlertingConfigExtendedForDisplay>(new Model<String>("Updated (GMT)"), "updateTimestamp", "updateTimestamp","columnWidth35"));

        columns.add(new FilteredAbstractColumn<AlertingConfigExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model<String>("filter"), new Model<String>("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<AlertingConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<AlertingConfigExtendedForDisplay> model) {
                cellItem.add(new ActionPanel(componentId, model));
            }

            @Override
            public String getCssClass() {
                return "columnWidth20";
            }
        });


        final FilterForm filterForm = new FilterForm("filterForm", dataProvider) {
            @Override
            protected void onSubmit() {
                dataTable.setCurrentPage(0);
            }
        };


        final int numRows = ((AreciboAlertManager) getApplication()).getConfig().getGeneralTableDisplayRows();
        dataTable = new AjaxFallbackDefaultDataTable<AlertingConfigExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable, filterForm, dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        newAlertConfigModalWindow = getAlertingConfigModalWindow("modalnewalertingconfig",INSERT);
        add(newAlertConfigModalWindow);

        add(new AjaxLink("addnewalertconfig") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                Session.get().getFeedbackMessages().clear();
                target.addComponent(feedback);

                newAlertConfigModalWindow.show(target);
            }
        });

        editAlertConfigModalWindow = getAlertingConfigModalWindow("modaleditalertingconfig",UPDATE);
        add(editAlertConfigModalWindow);

        deleteAlertConfigModalWindow = getAlertingConfigModalWindow("modaldeletealertingconfig",DELETE);
        add(deleteAlertConfigModalWindow);
    }

    private ModalWindowWithAlertingConfig getAlertingConfigModalWindow(String id, final ModalMode mode) {

        final ModalWindowWithAlertingConfig modalWindowWithAlertingConfig = new ModalWindowWithAlertingConfig(id);

        modalWindowWithAlertingConfig.setPageCreator(new ModalWindow.PageCreator() {

            @Override
            public Page createPage() {
                return new AlertingConfigModalPage(modalWindowWithAlertingConfig, AlertingConfigsPanel.this, new AlertingConfigFormModel(modalWindowWithAlertingConfig.alertingConfig), mode);
            }
        });

        return modalWindowWithAlertingConfig;
    }

    private List<AlertingConfigExtendedForDisplay> getAlertingConfigTableData(Map<Long,List<ConfDataNotifGroup>> notificationGroups,Map<Long,List<ConfDataManagingKey>> managingKeys) {

        try  {
            List<AlertingConfigExtendedForDisplay> alertingConfigs;

            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            alertingConfigs = confDataDAO.selectAll(AlertingConfigExtendedForDisplay.TYPE_NAME, AlertingConfigExtendedForDisplay.class);

            for (AlertingConfigExtendedForDisplay alertingConfig : alertingConfigs) {

                List<ConfDataNotifGroup> perAlertingConfigNotifGroups = notificationGroups.get(alertingConfig.getId());
                if (perAlertingConfigNotifGroups != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataNotifGroup notifGroup : perAlertingConfigNotifGroups) {
                        sb.append(String.format("%s$",notifGroup.getLabel()));
                    }
                    alertingConfig.setExtendedNotificationGroups(sb.toString());
                }

                List<ConfDataManagingKey> perAlertingConfigManagingKeys = managingKeys.get(alertingConfig.getId());
                if (perAlertingConfigManagingKeys != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataManagingKey managingKey : perAlertingConfigManagingKeys) {
                        sb.append(String.format("%s$",managingKey.getLabel()));
                    }
                    alertingConfig.setExtendedManagingKeys(sb.toString());
                }
            }


            return alertingConfigs;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            return new ArrayList<AlertingConfigExtendedForDisplay>();
        }
    }

    private Map<Long,List<ConfDataNotifGroup>> getNotificationGroupTableData() {

        HashMap<Long,List<ConfDataNotifGroup>> groupMap = new HashMap<Long,List<ConfDataNotifGroup>>();
        try  {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataNotifGroup> notificationGroups;
            notificationGroups = confDataDAO.selectAll(ConfDataNotifGroup.TYPE_NAME, ConfDataNotifGroup.class);
            Collections.sort(notificationGroups, ConfDataObjectByIdComparator.getInstance());

            List<ConfDataNotifGroupMapping> notificationGroupMappings;
            notificationGroupMappings = confDataDAO.selectAll(ConfDataNotifGroupMapping.TYPE_NAME, ConfDataNotifGroupMapping.class);


            // temporary wrapper for implementing binarySearch
            ConfDataNotifGroup searchConfig = new ConfDataNotifGroup();

            // add in all our notification groups per alerting config
            for(ConfDataNotifGroupMapping mapping:notificationGroupMappings) {

                List<ConfDataNotifGroup> perAlertingConfigNotifGroups = groupMap.get(mapping.getAlertingConfigId());
                if(perAlertingConfigNotifGroups == null) {
                    perAlertingConfigNotifGroups = new ArrayList<ConfDataNotifGroup>();
                    groupMap.put(mapping.getAlertingConfigId(),perAlertingConfigNotifGroups);
                }

                searchConfig.setId(mapping.getNotifGroupId());
                int foundIndex = Collections.binarySearch(notificationGroups,searchConfig,ConfDataObjectByIdComparator.getInstance());
                if(foundIndex >= 0) {
                    ConfDataNotifGroup group = notificationGroups.get(foundIndex);
                    perAlertingConfigNotifGroups.add(group);
                }
            }

            // sort each list per notif group
            for(List<ConfDataNotifGroup> perAlertingConfigNotifGroups:groupMap.values()) {
               Collections.sort(perAlertingConfigNotifGroups, ConfDataObjectByLabelComparator.getInstance());
            }

            return groupMap;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            groupMap.clear();
            return groupMap;
        }
    }

    private Map<Long,List<ConfDataManagingKey>> getManagingKeyTableData() {

        HashMap<Long,List<ConfDataManagingKey>> managingKeyMap = new HashMap<Long,List<ConfDataManagingKey>>();
        try  {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataManagingKey> managingKeys;
            managingKeys = confDataDAO.selectAll(ConfDataManagingKey.TYPE_NAME, ConfDataManagingKey.class);
            Collections.sort(managingKeys, ConfDataObjectByIdComparator.getInstance());

            List<ConfDataManagingKeyMapping> managingKeyMappings;
            managingKeyMappings = confDataDAO.selectAll(ConfDataManagingKeyMapping.TYPE_NAME, ConfDataManagingKeyMapping.class);


            // temporary wrapper for implementing binarySearch
            ConfDataManagingKey searchConfig = new ConfDataManagingKey();

            // add in all our managing keys per alerting config
            for(ConfDataManagingKeyMapping mapping:managingKeyMappings) {

                List<ConfDataManagingKey> perAlertingConfigManagingKeys = managingKeyMap.get(mapping.getAlertingConfigId());
                if(perAlertingConfigManagingKeys == null) {
                    perAlertingConfigManagingKeys = new ArrayList<ConfDataManagingKey>();
                    managingKeyMap.put(mapping.getAlertingConfigId(),perAlertingConfigManagingKeys);
                }

                searchConfig.setId(mapping.getManagingKeyId());
                int foundIndex = Collections.binarySearch(managingKeys,searchConfig,ConfDataObjectByIdComparator.getInstance());
                if(foundIndex >= 0) {
                    ConfDataManagingKey managingKey = managingKeys.get(foundIndex);
                    perAlertingConfigManagingKeys.add(managingKey);
                }
            }

            // sort each list per notif group
            for(List<ConfDataManagingKey> perAlertingConfigManagingKeys:managingKeyMap.values()) {
                Collections.sort(perAlertingConfigManagingKeys, ConfDataObjectByLabelComparator.getInstance());
            }

            return managingKeyMap;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            managingKeyMap.clear();
            return managingKeyMap;
        }
    }

    public void sendInfoFeedback(String message) {
        infoFeedbackMessage = message;
        errorFeedbackMessage = null;
    }

    public void sendErrorFeedback(String message) {
        infoFeedbackMessage = null;
        errorFeedbackMessage = message;
    }

    public void clearFeedback() {
        infoFeedbackMessage = null;
        errorFeedbackMessage = null;
    }

    private class ModalWindowWithAlertingConfig extends ModalWindow {
        private volatile AlertingConfigExtendedForDisplay alertingConfig = null;

        public ModalWindowWithAlertingConfig(String id) {
            super(id);

            setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                @Override
                public void onClose(AjaxRequestTarget target) {

                    if (infoFeedbackMessage != null) {
                        info(infoFeedbackMessage);

                        Map<Long, List<ConfDataNotifGroup>> newNotificationGroups = getNotificationGroupTableData();
                        notificationGroups.clear();
                        notificationGroups.putAll(newNotificationGroups);

                        Map<Long, List<ConfDataManagingKey>> newManagingKeys = getManagingKeyTableData();
                        managingKeys.clear();
                        managingKeys.putAll(newManagingKeys);

                        List<AlertingConfigExtendedForDisplay> updatedAlertingConfigs = getAlertingConfigTableData(notificationGroups,managingKeys);
                        dataProvider.updateDataList(updatedAlertingConfigs);
                        target.addComponent(dataTable);
                    }
                    else if (errorFeedbackMessage != null)
                        error(errorFeedbackMessage);

                    clearFeedback();

                    target.addComponent(feedback);
                }
            });

            setInitialHeight(MODAL_WINDOW_HEIGHT);
            setInitialWidth(MODAL_WINDOW_WIDTH);
        }

        public void setAlertingConfig(AlertingConfigExtendedForDisplay alertingConfig) {
            this.alertingConfig = alertingConfig;
        }
    }

    private class ActionPanel extends Panel {

        private AlertingConfigExtendedForDisplay selected;

        public ActionPanel(String id, IModel<AlertingConfigExtendedForDisplay> model) {
            super(id, model);
            add(new AjaxFallbackLink("edit") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (AlertingConfigExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    editAlertConfigModalWindow.setAlertingConfig(selected);
                    editAlertConfigModalWindow.show(target);
                }
            });

            add(new AjaxFallbackLink("delete") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (AlertingConfigExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    deleteAlertConfigModalWindow.setAlertingConfig(selected);
                    deleteAlertConfigModalWindow.show(target);
                }
            });
        }
    }

    private class NotificationGroupsPanel extends Panel {

        private AlertingConfigExtendedForDisplay selected;

        public NotificationGroupsPanel(String id, final IModel<AlertingConfigExtendedForDisplay> model) {
            super(id, model);

            AlertingConfigExtendedForDisplay alertingConfig = (AlertingConfigExtendedForDisplay) model.getObject();
            List<ConfDataNotifGroup> perAlertConfigNotifGroups = notificationGroups.get(alertingConfig.getId());

            if(perAlertConfigNotifGroups ==  null) {
                perAlertConfigNotifGroups = new ArrayList<ConfDataNotifGroup>();
            }

            add(new ListView<ConfDataNotifGroup>("notificationGroups",perAlertConfigNotifGroups) {

                @Override
                public void populateItem(final ListItem item) {
                    ConfDataNotifGroup group = (ConfDataNotifGroup)item.getModelObject();
                    item.add(new Label("groupName",group.getLabel()));
                }
            });
        }
    }

    private class ManagingKeysPanel extends Panel {

        private AlertingConfigExtendedForDisplay selected;

        public ManagingKeysPanel(String id, final IModel<AlertingConfigExtendedForDisplay> model) {
            super(id, model);

            AlertingConfigExtendedForDisplay alertingConfig = (AlertingConfigExtendedForDisplay) model.getObject();
            List<ConfDataManagingKey> perAlertingConfigManagingKeys = managingKeys.get(alertingConfig.getId());

            if(perAlertingConfigManagingKeys ==  null) {
                perAlertingConfigManagingKeys = new ArrayList<ConfDataManagingKey>();
            }

            add(new ListView<ConfDataManagingKey>("managingKeys",perAlertingConfigManagingKeys) {

                @Override
                public void populateItem(final ListItem item) {
                    ConfDataManagingKey key = (ConfDataManagingKey)item.getModelObject();
                    item.add(new Label("managingKey",key.getKey()));
                }
            });
        }
    }
}
