package com.ning.arecibo.alertmanager.tabs.alertincidents;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.PropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByUpdateTimestampComparator;

public class AlertIncidentLogTablePanel extends Panel {

    final FeedbackPanel feedback;
    final SortableConfDataProvider<AlertIncidentLogExtendedForDisplay> dataProvider;
    final Map<Long, ConfDataThresholdConfig> thresholdConfigs;
    final Map<Long, List<ConfDataAcknowledgementLog>> acknowledgementLogs;
    final List<IColumn<AlertIncidentLogExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<AlertIncidentLogExtendedForDisplay> dataTable;

    public AlertIncidentLogTablePanel(String id,final AlertIncidentSearchRangeModel searchRangeModel) {
        super(id);

        thresholdConfigs = getThresholdConfigTableData();
        acknowledgementLogs = getAcknowledgementTableData();
        List<AlertIncidentLogExtendedForDisplay> alertIncidentLogs = getAlertIncidentLogTableData(searchRangeModel, thresholdConfigs, acknowledgementLogs);

        dataProvider = new SortableConfDataProvider<AlertIncidentLogExtendedForDisplay>(alertIncidentLogs, "startTime", AlertIncidentLogExtendedForDisplay.class);

        columns = new ArrayList<IColumn<AlertIncidentLogExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertIncidentLogExtendedForDisplay>(new Model<String>("Threshold Config Name"), "extendedThresholdConfigName", "extendedThresholdConfigName", "columnWidth35"));
        columns.add(new TextFilteredPropertyColumnWithCssClass<AlertIncidentLogExtendedForDisplay>(new Model<String>("Context Data"), "extendedContextData", "extendedContextData", "columnWidth35"));
        columns.add(new PropertyColumnWithCssClass<AlertIncidentLogExtendedForDisplay>(new Model<String>("Initial Alert Value"), "initialAlertEventValue", "initialAlertEventValue", "columnWidth20"));
        columns.add(new PropertyColumnWithCssClass<AlertIncidentLogExtendedForDisplay>(new Model<String>("Short Description"), "shortDescription", "shortDescription", "columnWidth70"));
        columns.add(new TimestampPropertyColumn<AlertIncidentLogExtendedForDisplay>(new Model<String>("Start Time (GMT)"), "startTime", "startTime", "columnWidth35"));
        columns.add(new TimestampPropertyColumn<AlertIncidentLogExtendedForDisplay>(new Model<String>("Clear Time (GMT)"), "clearTime", "clearTime", "columnWidth35"));
        //columns.add(new TimestampPropertyColumn<AlertIncidentLogExtendedForDisplay>(new Model<String>("Acknowledgements"), "extendedAcknowledgementLogs", "extendedAcknowledgementLogs", "columnWidth50"));

        columns.add(new FilteredAbstractColumn<AlertIncidentLogExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model<String>("filter"), new Model<String>("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<AlertIncidentLogExtendedForDisplay>> cellItem, String componentId,
                                     IModel<AlertIncidentLogExtendedForDisplay> model) {
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
        dataTable = new AjaxFallbackDefaultDataTable<AlertIncidentLogExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable, filterForm, dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    public void updateTable(final AlertIncidentSearchRangeModel searchRangeModel) {

        Map<Long,ConfDataThresholdConfig> updatedThresholdConfigs = getThresholdConfigTableData();
        thresholdConfigs.clear();
        thresholdConfigs.putAll(updatedThresholdConfigs);

        Map<Long,List<ConfDataAcknowledgementLog>> updatedAcknowledgementLogs = getAcknowledgementTableData();
        acknowledgementLogs.clear();
        acknowledgementLogs.putAll(updatedAcknowledgementLogs);

        List<AlertIncidentLogExtendedForDisplay> alertIncidentLogs = getAlertIncidentLogTableData(searchRangeModel, thresholdConfigs, acknowledgementLogs);
        dataProvider.updateDataList(alertIncidentLogs);
    }

    private List<AlertIncidentLogExtendedForDisplay> getAlertIncidentLogTableData(AlertIncidentSearchRangeModel searchRangeModel,
                                                                                  Map<Long, ConfDataThresholdConfig> thresholdConfigs,
                                                                                  Map<Long, List<ConfDataAcknowledgementLog>> ackLogs) {

        try {

            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            List<AlertIncidentLogExtendedForDisplay> alertIncidentLogs =
                    confDataDAO.selectByDateColumnRange("start_time",
                            searchRangeModel.getSearchRangeStart().getTime(),
                            searchRangeModel.getSearchRangeEnd().getTime(),
                            AlertIncidentLogExtendedForDisplay.TYPE_NAME,
                            AlertIncidentLogExtendedForDisplay.class);

            for (AlertIncidentLogExtendedForDisplay alertIncidentLog : alertIncidentLogs) {

                Long thresholdConfigId = alertIncidentLog.getThresholdConfigId();
                if (thresholdConfigId != null) {
                    ConfDataThresholdConfig thresholdConfig = thresholdConfigs.get(thresholdConfigId);
                    alertIncidentLog.setExtendedThresholdConfigName(thresholdConfig.getLabel());
                }

                alertIncidentLog.setExtendedContextData(getDisplayableContextData(alertIncidentLog.getContextIdentifier()));

                List<ConfDataAcknowledgementLog> perAlertIncidentAckLogs = ackLogs.get(alertIncidentLog.getId());
                if (perAlertIncidentAckLogs != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataAcknowledgementLog ackLog : perAlertIncidentAckLogs) {
                        sb.append(String.format("%s$", ackLog.getLabel()));
                    }
                    alertIncidentLog.setExtendedAcknowledgementLogs(sb.toString());
                }
            }

            return alertIncidentLogs;
        }
        catch (ConfDataDAOException cddEx) {
            error(cddEx);

            return new ArrayList<AlertIncidentLogExtendedForDisplay>();
        }
    }

    private String getDisplayableContextData(String contextIdentifier) {
        if (contextIdentifier == null)
            return null;

        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(contextIdentifier, "&");

        // thow out the first one, which is the internal threshold config id
        if (st.hasMoreTokens()) {
            st.nextToken();
        }

        // do first one
        if (st.hasMoreTokens()) {
            sb.append(String.format("%s", st.nextToken()));
        }

        // do subsequent ones
        while (st.hasMoreTokens()) {
            sb.append(String.format("\n%s", st.nextToken()));
        }

        return sb.toString();
    }

    private Map<Long, ConfDataThresholdConfig> getThresholdConfigTableData() {

        HashMap<Long, ConfDataThresholdConfig> thresholdConfigMap = new HashMap<Long, ConfDataThresholdConfig>();
        try {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataThresholdConfig> thresholdConfigs = confDataDAO.selectAll(ConfDataThresholdConfig.TYPE_NAME, ConfDataThresholdConfig.class);

            // add in all our managing keys per alerting config
            for (ConfDataThresholdConfig thresholdConfig : thresholdConfigs) {
                thresholdConfigMap.put(thresholdConfig.getId(), thresholdConfig);
            }

            return thresholdConfigMap;
        }
        catch (ConfDataDAOException cddEx) {
            error(cddEx);

            thresholdConfigMap.clear();
            return thresholdConfigMap;
        }
    }

    private Map<Long, List<ConfDataAcknowledgementLog>> getAcknowledgementTableData() {

        HashMap<Long, List<ConfDataAcknowledgementLog>> ackLogMap = new HashMap<Long, List<ConfDataAcknowledgementLog>>();
        try {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataAcknowledgementLog> ackLogs = confDataDAO.selectAll(ConfDataAcknowledgementLog.TYPE_NAME, ConfDataAcknowledgementLog.class);

            // add in all our managing keys per alerting config
            for (ConfDataAcknowledgementLog ackLog : ackLogs) {

                List<ConfDataAcknowledgementLog> perAlertIncidentAckLogs = ackLogMap.get(ackLog.getAlertIncidentId());
                if (perAlertIncidentAckLogs == null) {
                    perAlertIncidentAckLogs = new ArrayList<ConfDataAcknowledgementLog>();
                    ackLogMap.put(ackLog.getAlertIncidentId(), perAlertIncidentAckLogs);
                }
            }

            // sort each list per notif group
            for (List<ConfDataAcknowledgementLog> perAlertIncidentAckLogs : ackLogMap.values()) {
                Collections.sort(perAlertIncidentAckLogs, ConfDataObjectByUpdateTimestampComparator.getInstance());
            }

            return ackLogMap;
        }
        catch (ConfDataDAOException cddEx) {
            error(cddEx);

            ackLogMap.clear();
            return ackLogMap;
        }
    }

    private class ActionPanel extends Panel {

        private AlertIncidentLogExtendedForDisplay selected;

        public ActionPanel(String id, IModel<AlertIncidentLogExtendedForDisplay> model) {
            super(id, model);
            
            /*
            add(new AjaxFallbackLink("acknowledge") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (AlertIncidentLogExtendedForDisplay) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    //editAlertConfigModalWindow.setAlertingConfig(selected);
                    //editAlertConfigModalWindow.show(target);
                }
            });
            */

            add(new EmptyPanel("acknowledge"));
        }
    }
}
