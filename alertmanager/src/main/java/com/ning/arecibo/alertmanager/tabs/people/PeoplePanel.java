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
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.ModalWindowUtils;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.NotificationConfigByAddressComparator;

import com.ning.arecibo.util.Logger;



import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class PeoplePanel extends Panel {
    final static Logger log = Logger.getLogger(PeoplePanel.class);

    final FeedbackPanel feedback;
    final SortableConfDataProvider<PersonExtendedForDisplay> dataProvider;
    final Map<Long,List<ConfDataNotifConfig>> notificationConfigs;
    final List<IColumn<PersonExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<PersonExtendedForDisplay> dataTable;
    final ModalWindow newPersonModalWindow;
    final ModalWindowWithPerson editPersonModalWindow;
    final ModalWindowWithPerson deletePersonModalWindow;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public PeoplePanel(String id) {
        super(id);


        final WebMarkupContainerWithAssociatedMarkup explainPanel1 = new WebMarkupContainerWithAssociatedMarkup("explanation1");
        explainPanel1.setVisible(false);
        explainPanel1.setOutputMarkupPlaceholderTag(true);
        add(explainPanel1);

        final WebMarkupContainerWithAssociatedMarkup explainPanel2 = new WebMarkupContainerWithAssociatedMarkup("explanation2");
        explainPanel2.setVisible(false);
        explainPanel2.setOutputMarkupPlaceholderTag(true);
        add(explainPanel2);

        final AjaxLink explainLink1 = new AjaxLink("explainLink1") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel1.setVisible(!explainPanel1.isVisible());
                explainPanel2.setVisible(false);
                target.addComponent(explainPanel1);
                target.addComponent(explainPanel2);
            }
        };
        add(explainLink1);

        final AjaxLink explainLink2 = new AjaxLink("explainLink2") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                explainPanel2.setVisible(!explainPanel2.isVisible());
                explainPanel1.setVisible(false);
                target.addComponent(explainPanel1);
                target.addComponent(explainPanel2);
            }
        };
        add(explainLink2);


        notificationConfigs = getNotificationConfigTableData();
        List<PersonExtendedForDisplay> persons = getPersonTableData(notificationConfigs);
        
        dataProvider = new SortableConfDataProvider<PersonExtendedForDisplay>(persons,"label",PersonExtendedForDisplay.class);

        columns = new ArrayList<IColumn<PersonExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<PersonExtendedForDisplay>(new Model<String>("Nickname"), "label", "label", "columnWidth35"));

        columns.add(new TextFilteredPropertyColumnWithCssClass<PersonExtendedForDisplay>(new Model<String>("Emails"), "extendedNotificationConfig",
                                                                                                           "extendedNotificationConfig","columnWidth70") {
            @Override
            public void populateItem(Item<ICellPopulator<PersonExtendedForDisplay>> cellItem, String componentId,
                                     IModel<PersonExtendedForDisplay> model) {
                cellItem.add(new NotificationConfigPanel(componentId, model));
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<PersonExtendedForDisplay>(new Model<String>("First Name"), "firstName", "firstName","columnWidth35"));
        columns.add(new TextFilteredPropertyColumnWithCssClass<PersonExtendedForDisplay>(new Model<String>("Last Name"), "lastName", "lastName","columnWidth35"));
        columns.add(new ChoiceFilteredPropertyColumn<PersonExtendedForDisplay,Boolean>(new Model<String>("Is Group Alias"),"isGroupAlias","isGroupAlias",
                                                                    new WildcardListModel<Boolean>(Arrays.asList((Boolean)null,Boolean.TRUE,Boolean.FALSE))));
        columns.add(new TimestampPropertyColumn<PersonExtendedForDisplay>(new Model<String>("Updated (GMT)"), "updateTimestamp", "updateTimestamp","columnWidth35"));
        
        columns.add(new FilteredAbstractColumn<PersonExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model("filter"), new Model("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<PersonExtendedForDisplay>> cellItem, String componentId,
                                     IModel<PersonExtendedForDisplay> model) {
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

        final int numRows = ((AreciboAlertManager)getApplication()).getConfig().getGeneralTableDisplayRows();
        dataTable = new AjaxFallbackDefaultDataTable<PersonExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable,filterForm,dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        newPersonModalWindow = getPersonModalWindow("modalnewperson",INSERT);
        add(newPersonModalWindow);

        add(new AjaxLink("addnewperson") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                Session.get().getFeedbackMessages().clear();
                target.addComponent(feedback);
                
                newPersonModalWindow.show(target);
            }
        });

        editPersonModalWindow = getPersonModalWindow("modaleditperson",UPDATE);
        add(editPersonModalWindow);

        deletePersonModalWindow = getPersonModalWindow("modaldeleteperson",DELETE);
        add(deletePersonModalWindow);
    }

    private ModalWindowWithPerson getPersonModalWindow(String id, final ModalWindowUtils.ModalMode mode) {

        final ModalWindowWithPerson modalWindowWithPerson = new ModalWindowWithPerson(id);

        modalWindowWithPerson.setPageCreator(new ModalWindow.PageCreator() {

            @Override
            public Page createPage() {
                return new PersonModalPage(modalWindowWithPerson, PeoplePanel.this, new PersonFormModel(modalWindowWithPerson.person), mode);
            }
        });

        return modalWindowWithPerson;
    }

    private List<PersonExtendedForDisplay> getPersonTableData(Map<Long,List<ConfDataNotifConfig>> notificationConfigs) {

        try  {
            List<PersonExtendedForDisplay> persons;

            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            persons = confDataDAO.selectAll(PersonExtendedForDisplay.TYPE_NAME, PersonExtendedForDisplay.class);

            for(PersonExtendedForDisplay person:persons) {
                // get the notificationConfigs
                List<ConfDataNotifConfig> perPersonNotifConfigs = notificationConfigs.get(person.getId());
                if(perPersonNotifConfigs != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataNotifConfig perPersonNotifConfig : perPersonNotifConfigs) {
                        sb.append(perPersonNotifConfig.getAddress());
                        sb.append("(");
                        sb.append(perPersonNotifConfig.getNotifType().toString());
                        sb.append(")");
                    }
                    person.setExtendedNotificationConfig(sb.toString());
                }
            }

            return persons;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            return new ArrayList<PersonExtendedForDisplay>();
        }
    }

    private Map<Long,List<ConfDataNotifConfig>> getNotificationConfigTableData() {

        HashMap<Long,List<ConfDataNotifConfig>> configMap = new HashMap<Long,List<ConfDataNotifConfig>>();
        try  {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataNotifConfig> notificationConfigs;
            notificationConfigs = confDataDAO.selectAll(ConfDataNotifConfig.TYPE_NAME, ConfDataNotifConfig.class);

            // add in all our notifications per person
            for(ConfDataNotifConfig config:notificationConfigs) {

                List<ConfDataNotifConfig> perPersonConfigs = configMap.get(config.getPersonId());
                if(perPersonConfigs == null) {
                    perPersonConfigs = new ArrayList<ConfDataNotifConfig>();
                    configMap.put(config.getPersonId(),perPersonConfigs);
                }
                perPersonConfigs.add(config);
            }

            // sort each list per person
            for(List<ConfDataNotifConfig> perPersonConfigs:configMap.values()) {
               Collections.sort(perPersonConfigs, NotificationConfigByAddressComparator.getInstance());
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

    private class ModalWindowWithPerson extends ModalWindow {
        private volatile ConfDataPerson person = null;

        public ModalWindowWithPerson(String id) {
            super(id);

            setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                @Override
                public void onClose(AjaxRequestTarget target) {

                    if (infoFeedbackMessage != null) {
                        info(infoFeedbackMessage);

                        Map<Long, List<ConfDataNotifConfig>> newNotificationConfigs = getNotificationConfigTableData();
                        notificationConfigs.clear();
                        notificationConfigs.putAll(newNotificationConfigs);

                        List<PersonExtendedForDisplay> updatedPersons = getPersonTableData(notificationConfigs);
                        dataProvider.updateDataList(updatedPersons);
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

        public void setPerson(ConfDataPerson person) {
            this.person = person;
        }
    }

    private class ActionPanel extends Panel {

        private PersonExtendedForDisplay selected;

        public ActionPanel(String id, IModel<PersonExtendedForDisplay> model) {
            super(id, model);
            add(new AjaxFallbackLink("edit") {

                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (PersonExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    editPersonModalWindow.setPerson(selected);
                    editPersonModalWindow.show(target);
                }
            });

            add(new AjaxFallbackLink("delete") {

                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (PersonExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    deletePersonModalWindow.setPerson(selected);
                    deletePersonModalWindow.show(target);
                }
            });
        }
    }

    private class NotificationConfigPanel extends Panel {

        private PersonExtendedForDisplay selected;

        public NotificationConfigPanel(String id, final IModel<PersonExtendedForDisplay> model) {
            super(id, model);

            PersonExtendedForDisplay person = (PersonExtendedForDisplay) model.getObject();
            List<ConfDataNotifConfig> perPersonConfigs = notificationConfigs.get(person.getId());

            if(perPersonConfigs ==  null) {
                perPersonConfigs = new ArrayList<ConfDataNotifConfig>();
            }

            add(new ListView<ConfDataNotifConfig>("notifications",perPersonConfigs) {

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
