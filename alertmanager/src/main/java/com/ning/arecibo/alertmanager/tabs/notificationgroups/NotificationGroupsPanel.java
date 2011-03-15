package com.ning.arecibo.alertmanager.tabs.notificationgroups;

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
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.ChoiceFilteredPropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifMapping;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.ModalWindowUtils;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByIdComparator;
import com.ning.arecibo.alertmanager.utils.comparators.NotificationConfigByAddressComparator;

import com.ning.arecibo.util.Logger;



import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class NotificationGroupsPanel extends Panel {
    final static Logger log = Logger.getLogger(NotificationGroupsPanel.class);

    final FeedbackPanel feedback;
    final SortableConfDataProvider<NotifGroupExtendedForDisplay> dataProvider;
    final Map<Long,List<ConfDataNotifConfig>> notificationConfigs;
    final List<IColumn<NotifGroupExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<NotifGroupExtendedForDisplay> dataTable;
    final ModalWindow newNotifGroupModalWindow;
    final ModalWindowWithNotifGroup editNotifGroupModalWindow;
    final ModalWindowWithNotifGroup deleteNotifGroupModalWindow;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public NotificationGroupsPanel(String id) {
        super(id);

        final WebMarkupContainerWithAssociatedMarkup explainPanel1 = new WebMarkupContainerWithAssociatedMarkup("explanation1");
        explainPanel1.setVisible(false);
        explainPanel1.setOutputMarkupPlaceholderTag(true);
        add(explainPanel1);


        final AjaxLink explainLink1 = new AjaxLink("explainLink1") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel1.setVisible(!explainPanel1.isVisible());
                target.addComponent(explainPanel1);
            }
        };
        add(explainLink1);


        notificationConfigs = getNotificationConfigTableData();
        List<NotifGroupExtendedForDisplay> notifGroups = getNotifGroupTableData(notificationConfigs);
        dataProvider = new SortableConfDataProvider<NotifGroupExtendedForDisplay>(notifGroups,"label",NotifGroupExtendedForDisplay.class);

        columns = new ArrayList<IColumn<NotifGroupExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<NotifGroupExtendedForDisplay>(new Model<String>("Group Name"), "label", "label", "columnWidth35"));

        columns.add(new TextFilteredPropertyColumnWithCssClass<NotifGroupExtendedForDisplay>(new Model<String>("Emails"),"extendedNotificationConfigs","extendedNotificationConfigs","columnWidth70") {

            @Override
            public void populateItem(Item<ICellPopulator<NotifGroupExtendedForDisplay>> cellItem, String componentId,
                                     IModel<NotifGroupExtendedForDisplay> model) {
                cellItem.add(new NotificationConfigPanel(componentId, model));
            }
        });

        columns.add(new ChoiceFilteredPropertyColumn<NotifGroupExtendedForDisplay, Boolean>(new Model<String>("Is Enabled"), "enabled", "enabled",
                new WildcardListModel<Boolean>(Arrays.asList((Boolean) null, Boolean.TRUE, Boolean.FALSE))));

        columns.add(new TimestampPropertyColumn<NotifGroupExtendedForDisplay>(new Model<String>("Updated (GMT)"), "updateTimestamp", "updateTimestamp","columnWidth35"));

        columns.add(new FilteredAbstractColumn<NotifGroupExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model("filter"), new Model("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<NotifGroupExtendedForDisplay>> cellItem, String componentId,
                                     IModel<NotifGroupExtendedForDisplay> model) {
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

        final int numRows = ((AreciboAlertManager) getApplication()).getConfigProps().getGeneralTableDisplayRows();
        dataTable = new AjaxFallbackDefaultDataTable<NotifGroupExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable, filterForm, dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        newNotifGroupModalWindow = getNotifGroupModalWindow("modalnewnotifgroup",INSERT);
        add(newNotifGroupModalWindow);

        add(new AjaxLink("addnewnotifgroup") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                Session.get().getFeedbackMessages().clear();
                target.addComponent(feedback);

                newNotifGroupModalWindow.show(target);
            }
        });

        editNotifGroupModalWindow = getNotifGroupModalWindow("modaleditnotifgroup",UPDATE);
        add(editNotifGroupModalWindow);

        deleteNotifGroupModalWindow = getNotifGroupModalWindow("modaldeletenotifgroup",DELETE);
        add(deleteNotifGroupModalWindow);
    }

    private ModalWindowWithNotifGroup getNotifGroupModalWindow(String id, final ModalMode mode) {

        final ModalWindowWithNotifGroup modalWindowWithNotifGroup = new ModalWindowWithNotifGroup(id);

        modalWindowWithNotifGroup.setPageCreator(new ModalWindow.PageCreator() {

            @Override
            public Page createPage() {
                return new NotificationGroupModalPage(modalWindowWithNotifGroup, NotificationGroupsPanel.this, new NotificationGroupFormModel(modalWindowWithNotifGroup.notifGroup), mode);
            }
        });

        return modalWindowWithNotifGroup;
    }

    private List<NotifGroupExtendedForDisplay> getNotifGroupTableData(Map<Long,List<ConfDataNotifConfig>> notificationConfigs) {

        try  {
            List<NotifGroupExtendedForDisplay> notifGroups;
            
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            notifGroups = confDataDAO.selectAll(NotifGroupExtendedForDisplay.TYPE_NAME, NotifGroupExtendedForDisplay.class);

            for(NotifGroupExtendedForDisplay notifGroup:notifGroups) {
                List<ConfDataNotifConfig> perNotifGroupConfigs = notificationConfigs.get(notifGroup.getId());
                if(perNotifGroupConfigs != null) {
                    StringBuilder sb = new StringBuilder();
                    for(ConfDataNotifConfig notifConfig:perNotifGroupConfigs) {
                        sb.append(notifConfig.getAddress());
                        sb.append("(");
                        sb.append(notifConfig.getNotifType().toString());
                        sb.append(")");
                    }

                    notifGroup.setExtendedNotificationConfigs(sb.toString());
                }
            }

            return notifGroups;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            return new ArrayList<NotifGroupExtendedForDisplay>();
        }
    }

    private Map<Long,List<ConfDataNotifConfig>> getNotificationConfigTableData() {

        HashMap<Long,List<ConfDataNotifConfig>> configMap = new HashMap<Long,List<ConfDataNotifConfig>>();
        try  {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            
            List<ConfDataNotifConfig> notificationConfigs;
            notificationConfigs = confDataDAO.selectAll(ConfDataNotifConfig.TYPE_NAME, ConfDataNotifConfig.class);
            Collections.sort(notificationConfigs, ConfDataObjectByIdComparator.getInstance());

            List<ConfDataNotifMapping> notificationMappings;
            notificationMappings = confDataDAO.selectAll(ConfDataNotifMapping.TYPE_NAME, ConfDataNotifMapping.class);


            // temporary wrapper for implementing binarySearch
            ConfDataNotifConfig searchConfig = new ConfDataNotifConfig();

            // add in all our notifications per notif group
            for(ConfDataNotifMapping mapping:notificationMappings) {

                List<ConfDataNotifConfig> perNotifGroupConfigs = configMap.get(mapping.getNotifGroupId());
                if(perNotifGroupConfigs == null) {
                    perNotifGroupConfigs = new ArrayList<ConfDataNotifConfig>();
                    configMap.put(mapping.getNotifGroupId(),perNotifGroupConfigs);
                }

                searchConfig.setId(mapping.getNotifConfigId());
                int foundIndex = Collections.binarySearch(notificationConfigs,searchConfig,ConfDataObjectByIdComparator.getInstance());
                if(foundIndex >= 0) {
                    ConfDataNotifConfig config = notificationConfigs.get(foundIndex);
                    perNotifGroupConfigs.add(config);
                }
            }

            // sort each list per notif group
            for(List<ConfDataNotifConfig> perNotifGroupConfigs:configMap.values()) {
               Collections.sort(perNotifGroupConfigs,NotificationConfigByAddressComparator.getInstance());
            }

            return configMap;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            configMap.clear();
            return configMap;
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

    private class ModalWindowWithNotifGroup extends ModalWindow {
        private volatile ConfDataNotifGroup notifGroup = null;

        public ModalWindowWithNotifGroup(String id) {
            super(id);

            setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                @Override
                public void onClose(AjaxRequestTarget target) {

                    if (infoFeedbackMessage != null) {
                        info(infoFeedbackMessage);

                        Map<Long, List<ConfDataNotifConfig>> newNotificationConfigs = getNotificationConfigTableData();
                        notificationConfigs.clear();
                        notificationConfigs.putAll(newNotificationConfigs);

                        List<NotifGroupExtendedForDisplay> updatedNotifGroups = getNotifGroupTableData(notificationConfigs);
                        dataProvider.updateDataList(updatedNotifGroups);
                        target.addComponent(dataTable);
                    }
                    else if (errorFeedbackMessage != null)
                        error(errorFeedbackMessage);

                    clearFeedback();

                    target.addComponent(feedback);
                }
            });

            setInitialHeight(ModalWindowUtils.MODAL_WINDOW_HEIGHT);
            setInitialWidth(ModalWindowUtils.MODAL_WINDOW_WIDTH);
        }

        public void setNotifGroup(ConfDataNotifGroup notifGroup) {
            this.notifGroup = notifGroup;
        }
    }

    private class ActionPanel extends Panel {

        private NotifGroupExtendedForDisplay selected;

        public ActionPanel(String id, IModel<NotifGroupExtendedForDisplay> model) {
            super(id, model);
            add(new AjaxFallbackLink("edit") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (NotifGroupExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    editNotifGroupModalWindow.setNotifGroup(selected);
                    editNotifGroupModalWindow.show(target);
                }
            });

            add(new AjaxFallbackLink("delete") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (NotifGroupExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    deleteNotifGroupModalWindow.setNotifGroup(selected);
                    deleteNotifGroupModalWindow.show(target);
                }
            });
        }
    }

    private class NotificationConfigPanel extends Panel {

        private ConfDataNotifGroup selected;

        public NotificationConfigPanel(String id, final IModel<NotifGroupExtendedForDisplay> model) {
            super(id, model);

            ConfDataNotifGroup notifGroup = (ConfDataNotifGroup) model.getObject();
            List<ConfDataNotifConfig> perNotifGroupConfigs = notificationConfigs.get(notifGroup.getId());

            if(perNotifGroupConfigs ==  null) {
                perNotifGroupConfigs = new ArrayList<ConfDataNotifConfig>();
            }

            add(new ListView<ConfDataNotifConfig>("notifications",perNotifGroupConfigs) {

                @Override
                public void populateItem(final ListItem item) {
                    ConfDataNotifConfig config = (ConfDataNotifConfig)item.getModelObject();
                    item.add(new Label("address",config.getAddress()));
                    item.add(new Label("notifType","(" + config.getNotifType().toString() + ")"));
                }
            });
        }
    }
}
