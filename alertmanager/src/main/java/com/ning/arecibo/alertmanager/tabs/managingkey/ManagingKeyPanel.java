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

package com.ning.arecibo.alertmanager.tabs.managingkey;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
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
import com.ning.arecibo.alert.confdata.enums.ManagingKeyActionType;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.DayOfWeekUtils;
import com.ning.arecibo.alertmanager.utils.ModalWindowUtils;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.TimeOfDayUtils;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ManagingKeyPanel extends Panel {
    final static Logger log = Logger.getLogger(ManagingKeyPanel.class);

    final FeedbackPanel feedback;
    final SortableConfDataProvider<ManagingKeyExtendedForDisplay> dataProvider;
    final List<IColumn<ManagingKeyExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<ManagingKeyExtendedForDisplay> dataTable;
    
    final ModalWindow newManagingKeyModalWindow;
    final ModalWindowWithManagingKey editManagingKeyModalWindow;
    final ModalWindowWithManagingKey deleteManagingKeyModalWindow;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public ManagingKeyPanel(String id) {
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



        List<ManagingKeyExtendedForDisplay> managingKeys = getManagingKeyTableData();
        dataProvider = new SortableConfDataProvider<ManagingKeyExtendedForDisplay>(managingKeys,"key",ManagingKeyExtendedForDisplay.class);

        columns = new ArrayList<IColumn<ManagingKeyExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<ManagingKeyExtendedForDisplay>(new Model<String>("Managing Rule"), "key", "key", "columnWidth20"));

        columns.add(new ChoiceFilteredPropertyColumn<ManagingKeyExtendedForDisplay, ManagingKeyActionType>(new Model<String>("Suppress Action"), "action", "action",
                new WildcardListModel<ManagingKeyActionType>(Arrays.asList((ManagingKeyActionType)null,ManagingKeyActionType.NO_ACTION,ManagingKeyActionType.QUIESCE,ManagingKeyActionType.DISABLE)))); 

        columns.add(new TextFilteredPropertyColumnWithCssClass<ManagingKeyExtendedForDisplay>(new Model<String>("Schedule by Time of Day"),"extendedScheduleByTimeOfDay","extendedScheduleByTimeOfDay","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ManagingKeyExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ManagingKeyExtendedForDisplay> model) {
                cellItem.add(new ScheduleByTimeOfDayPanel(componentId, model));
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<ManagingKeyExtendedForDisplay>(new Model<String>("Schedule by Day of Week"),"extendedScheduleByDayOfWeek","extendedScheduleByDayOfWeek","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ManagingKeyExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ManagingKeyExtendedForDisplay> model) {
                cellItem.add(new ScheduleByDayOfWeekPanel(componentId, model));
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<ManagingKeyExtendedForDisplay>(new Model<String>("Manual Invocation Settings"),"extendedManualInvocationSettings","extendedManualInvocationSettings","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ManagingKeyExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ManagingKeyExtendedForDisplay> model) {
                cellItem.add(new ManualInvocationPanel(componentId, model));
            }
        });

        columns.add(new TimestampPropertyColumn<ManagingKeyExtendedForDisplay>(new Model<String>("Updated (GMT)"), "updateTimestamp", "updateTimestamp", "columnWidth35"));

        columns.add(new FilteredAbstractColumn<ManagingKeyExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model("filter"), new Model("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<ManagingKeyExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ManagingKeyExtendedForDisplay> model) {
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
        dataTable = new AjaxFallbackDefaultDataTable<ManagingKeyExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable, filterForm, dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        newManagingKeyModalWindow = getManagingKeyModalWindow("modalnewmanagingkey",INSERT);
        add(newManagingKeyModalWindow);

        add(new AjaxLink("addnewmanagingkey") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                Session.get().getFeedbackMessages().clear();
                target.addComponent(feedback);

                newManagingKeyModalWindow.show(target);
            }
        });

        editManagingKeyModalWindow = getManagingKeyModalWindow("modaleditmanagingkey",UPDATE);
        add(editManagingKeyModalWindow);

        deleteManagingKeyModalWindow = getManagingKeyModalWindow("modaldeletemanagingkey",DELETE);
        add(deleteManagingKeyModalWindow);
    }

    private ModalWindowWithManagingKey getManagingKeyModalWindow(String id,final ModalMode mode) {

        final ModalWindowWithManagingKey modalWindowWithManagingKey = new ModalWindowWithManagingKey(id);

        modalWindowWithManagingKey.setPageCreator(new ModalWindow.PageCreator() {

            @Override
            public Page createPage() {
                return new ManagingKeyModalPage(modalWindowWithManagingKey, ManagingKeyPanel.this, new ManagingKeyFormModel(modalWindowWithManagingKey.managingKey),mode);
            }
        });

        return modalWindowWithManagingKey;
    }

    private List<ManagingKeyExtendedForDisplay> getManagingKeyTableData() {

        try  {
            List<ManagingKeyExtendedForDisplay> managingKeys;
            
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            managingKeys = confDataDAO.selectAll(ManagingKeyExtendedForDisplay.TYPE_NAME, ManagingKeyExtendedForDisplay.class);

            for(ManagingKeyExtendedForDisplay managingKey:managingKeys) {

                // set scheduleTimeOfDay
                String scheduleTimeOfDay;
                if (managingKey.getAutoActivateTODStartMs() == null || managingKey.getAutoActivateTODEndMs() == null) {
                    scheduleTimeOfDay = null;
                }
                else {
                    scheduleTimeOfDay = TimeOfDayUtils.format(managingKey.getAutoActivateTODStartMs()) + " to " +
                            TimeOfDayUtils.format(managingKey.getAutoActivateTODEndMs()) + " GMT";
                }
                managingKey.setExtendedScheduleByTimeOfDay(scheduleTimeOfDay);


                // set scheduleByDayOfWeek
                String scheduleDayOfWeek;
                if (managingKey.getAutoActivateDOWStart() == null || managingKey.getAutoActivateDOWEnd() == null) {
                    scheduleDayOfWeek = null;
                }
                else {
                    scheduleDayOfWeek = DayOfWeekUtils.format(managingKey.getAutoActivateDOWStart().intValue()) + " to " +
                            DayOfWeekUtils.format(managingKey.getAutoActivateDOWEnd().intValue()) + " GMT";
                }
                managingKey.setExtendedScheduleByDayOfWeek(scheduleDayOfWeek);


                // set manualInvocation
                String invocationString;
                if (managingKey.getActivatedIndefinitely() != null && managingKey.getActivatedIndefinitely()) {
                    invocationString = "always invoked";
                }
                else if (managingKey.getActivatedUntilTs() != null) {
                    invocationString = "invoked until " + TimestampPropertyColumn.format(managingKey.getActivatedUntilTs(), "yyyy-MM-dd HH:mm") + " GMT";
                }
                else
                    invocationString = null;

                managingKey.setExtendedManualInvocationSettings(invocationString);

            }

            return managingKeys;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            return new ArrayList<ManagingKeyExtendedForDisplay>();
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

    private class ModalWindowWithManagingKey extends ModalWindow {
        private volatile ConfDataManagingKey managingKey = null;

        public ModalWindowWithManagingKey(String id) {
            super(id);

            setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                @Override
                public void onClose(AjaxRequestTarget target) {

                    if (infoFeedbackMessage != null) {
                        info(infoFeedbackMessage);

                        List<ManagingKeyExtendedForDisplay> updatedManagingKeys = getManagingKeyTableData();
                        dataProvider.updateDataList(updatedManagingKeys);
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

        public void setManagingKey(ConfDataManagingKey managingKey) {
            this.managingKey = managingKey;
        }
    }

    private class ActionPanel extends Panel {

        private ManagingKeyExtendedForDisplay selected;

        public ActionPanel(String id, IModel<ManagingKeyExtendedForDisplay> model) {
            super(id, model);
            add(new AjaxFallbackLink("edit") {

                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (ManagingKeyExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    editManagingKeyModalWindow.setManagingKey(selected);
                    editManagingKeyModalWindow.show(target);
                }
            });

            add(new AjaxFallbackLink("delete") {

                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (ManagingKeyExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    deleteManagingKeyModalWindow.setManagingKey(selected);
                    deleteManagingKeyModalWindow.show(target);
                }
            });
        }
    }

    private class ManualInvocationPanel extends Panel {

        private ManagingKeyExtendedForDisplay selected;

        public ManualInvocationPanel(String id, final IModel<ManagingKeyExtendedForDisplay> model) {
            super(id, model);

            ManagingKeyExtendedForDisplay managingKey = (ManagingKeyExtendedForDisplay) model.getObject();

            final Label manualInvocation = new Label("manualInvocation",managingKey.getExtendedManualInvocationSettings());
            add(manualInvocation);
        }
    }

    private class ScheduleByTimeOfDayPanel extends Panel {

        private ManagingKeyExtendedForDisplay selected;

        public ScheduleByTimeOfDayPanel(String id, final IModel<ManagingKeyExtendedForDisplay> model) {
            super(id, model);

            ManagingKeyExtendedForDisplay managingKey = (ManagingKeyExtendedForDisplay) model.getObject();

            final Label scheduleByTimeOfDay = new Label("scheduleByTimeOfDay",managingKey.getExtendedScheduleByTimeOfDay());
            add(scheduleByTimeOfDay);
        }
    }

    private class ScheduleByDayOfWeekPanel extends Panel {

        private ManagingKeyExtendedForDisplay selected;

        public ScheduleByDayOfWeekPanel(String id, final IModel<ManagingKeyExtendedForDisplay> model) {
            super(id, model);

            ManagingKeyExtendedForDisplay managingKey = (ManagingKeyExtendedForDisplay) model.getObject();

            final Label scheduleByDayOfWeek = new Label("scheduleByDayOfWeek",managingKey.getExtendedScheduleByDayOfWeek());
            add(scheduleByDayOfWeek);
        }
    }
}
