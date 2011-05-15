package com.ning.arecibo.alertmanager.tabs.thresholds;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.Session;
import org.apache.wicket.Page;
import org.apache.wicket.Component;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.ModalWindowUtils;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.ThresholdContextAttrByAttributeTypeComparator;
import com.ning.arecibo.alertmanager.utils.comparators.ThresholdQualifyingAttrByAttributeTypeComparator;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ThresholdsPanel extends Panel {
    final static Logger log = Logger.getLogger(ThresholdsPanel.class);

    final FeedbackPanel feedback;
    final SortableConfDataProvider<ThresholdConfigExtendedForDisplay> dataProvider;
    final Map<Long, ConfDataAlertingConfig> alertingConfigs;
    final Map<Long,List<ConfDataThresholdContextAttr>> contextAttrs;
    final Map<Long,List<ConfDataThresholdQualifyingAttr>> qualifyingAttrs;
    final List<IColumn<ThresholdConfigExtendedForDisplay>> columns;
    final AjaxFallbackDefaultDataTable<ThresholdConfigExtendedForDisplay> dataTable;
    final ModalWindow newThresholdModalWindow;
    final ModalWindowWithThreshold editThresholdModalWindow;
    final ModalWindowWithThreshold deleteThresholdModalWindow;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public ThresholdsPanel(String id) {
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

        alertingConfigs = getAlertingConfigTableData();
        contextAttrs = getThresholdContextAttrTableData();
        qualifyingAttrs = getThresholdQualifyingAttrTableData();
        List<ThresholdConfigExtendedForDisplay> thresholdConfigs = getThresholdConfigTableData(alertingConfigs,contextAttrs,qualifyingAttrs);

        dataProvider = new SortableConfDataProvider<ThresholdConfigExtendedForDisplay>(thresholdConfigs,"label", ThresholdConfigExtendedForDisplay.class);

        columns = new ArrayList<IColumn<ThresholdConfigExtendedForDisplay>>();
        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Threshold Name"), "label", "label", "columnWidth35"));
        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Event Type"), "monitoredEventType", "monitoredEventType", "columnWidth35"));
        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Attribute Type"), "monitoredAttributeType", "monitoredAttributeType", "columnWidth35"));

        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Qualifying Attributes"),"extendedQualifyingAttributes","extendedQualifyingAttributes","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ThresholdConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ThresholdConfigExtendedForDisplay> model) {
                cellItem.add(new ThresholdQualifyingAttrsPanel(componentId, model));
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Context Attributes"),"extendedContextAttributes","extendedContextAttributes","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ThresholdConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ThresholdConfigExtendedForDisplay> model) {
                cellItem.add(new ThresholdContextAttrsPanel(componentId, model));
            }
        });

        columns.add(new AbstractColumn<ThresholdConfigExtendedForDisplay>(new Model<String>("Options")) {

            @Override
            public void populateItem(Item<ICellPopulator<ThresholdConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ThresholdConfigExtendedForDisplay> model) {
                cellItem.add(new OptionsPanel(componentId, model));
            }

            @Override
            public String getCssClass() {
                return "columnWidth50";
            }
        });

        columns.add(new TextFilteredPropertyColumnWithCssClass<ThresholdConfigExtendedForDisplay>(new Model<String>("Alerting Config"),"extendedAlertingConfig","extendedAlertingConfig","columnWidth35") {

            @Override
            public void populateItem(Item<ICellPopulator<ThresholdConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ThresholdConfigExtendedForDisplay> model) {
                
                ThresholdConfigExtendedForDisplay config = model.getObject();
                ConfDataAlertingConfig alertingConfig = alertingConfigs.get(config.getAlertingConfigId());
                if(alertingConfig != null)
                    cellItem.add(new Label(componentId, alertingConfig.getLabel()));
                else
                    cellItem.add(new Label(componentId, "Not defined"));
            }
        });

        columns.add(new TimestampPropertyColumn<ThresholdConfigExtendedForDisplay>(new Model<String>("Updated (GMT)"), "updateTimestamp", "updateTimestamp","columnWidth35"));

        columns.add(new FilteredAbstractColumn<ThresholdConfigExtendedForDisplay>(new Model<String>("Actions")) {

            @Override
            public Component getFilter(String componentId, FilterForm form) {
                return new GoAndClearFilter(componentId, form, new Model("filter"), new Model("clear"));
            }

            @Override
            public void populateItem(Item<ICellPopulator<ThresholdConfigExtendedForDisplay>> cellItem, String componentId,
                                     IModel<ThresholdConfigExtendedForDisplay> model) {
                cellItem.add(new ActionPanel(componentId, model));
            }

            @Override
            public String getCssClass() {
                return "columnWidth20";
            }
        });

        final FilterForm filterForm = new FilterForm("filterForm", dataProvider)
        {
            @Override
            protected void onSubmit()
            {
                dataTable.setCurrentPage(0);
            }
        };

        final int numRows = ((AreciboAlertManager)getApplication()).getConfig().getThresholdsTableDisplayRows();
        dataTable = new AjaxFallbackDefaultDataTable<ThresholdConfigExtendedForDisplay>("table", columns, dataProvider, numRows);
        dataTable.setOutputMarkupPlaceholderTag(true);

        dataTable.addTopToolbar(new FilterToolbar(dataTable,filterForm,dataProvider));
        filterForm.add(dataTable);
        add(filterForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        newThresholdModalWindow = getThresholdModalWindow("modalnewthreshold",INSERT);
        add(newThresholdModalWindow);

        add(new AjaxLink("addnewthreshold") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                Session.get().getFeedbackMessages().clear();
                target.addComponent(feedback);

                newThresholdModalWindow.show(target);
            }
        });

        editThresholdModalWindow = getThresholdModalWindow("modaleditthreshold",UPDATE);
        add(editThresholdModalWindow);

        deleteThresholdModalWindow = getThresholdModalWindow("modaldeletethreshold",DELETE);
        add(deleteThresholdModalWindow);
    }

    private ModalWindowWithThreshold getThresholdModalWindow(String id, final ModalWindowUtils.ModalMode mode) {

        final ModalWindowWithThreshold modalWindowWithThreshold = new ModalWindowWithThreshold(id);

        modalWindowWithThreshold.setPageCreator(new ModalWindow.PageCreator() {

            @Override
            public Page createPage() {
                return new ThresholdModalPage(modalWindowWithThreshold,ThresholdsPanel.this, new ThresholdFormModel(modalWindowWithThreshold.threshold), mode);
            }
        });

        return modalWindowWithThreshold;
    }

    private List<ThresholdConfigExtendedForDisplay> getThresholdConfigTableData(Map<Long,ConfDataAlertingConfig> alertingConfigs,
                                                                                Map<Long,List<ConfDataThresholdContextAttr>> contextAttrs,
                                                                                Map<Long,List<ConfDataThresholdQualifyingAttr>> qualifyingAttrs) {

        try  {
            List<ThresholdConfigExtendedForDisplay> thresholdConfigs;
            
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            thresholdConfigs = confDataDAO.selectAll(ThresholdConfigExtendedForDisplay.TYPE_NAME, ThresholdConfigExtendedForDisplay.class);

            // add in extended fields for display/filtering
            for(ThresholdConfigExtendedForDisplay config:thresholdConfigs) {

                // get the alertingConfig
                if(config.getAlertingConfigId() != null) {
                    ConfDataAlertingConfig alertingConfig = alertingConfigs.get(config.getAlertingConfigId());
                    if(alertingConfig != null)
                        config.setExtendedAlertingConfig(alertingConfig.getLabel());
                }

                // get the context attributes
                List<ConfDataThresholdContextAttr> perThresholdContextAttrs = contextAttrs.get(config.getId());
                if(perThresholdContextAttrs != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataThresholdContextAttr perThresholdContextAttr : perThresholdContextAttrs) {
                        sb.append(String.format("%s$", perThresholdContextAttr.getAttributeType()));
                    }
                    config.setExtendedContextAttributes(sb.toString());
                }

                // get the qualifying attributes
                List<ConfDataThresholdQualifyingAttr> perThresholdQualifyingAttrs = qualifyingAttrs.get(config.getId());
                if(perThresholdQualifyingAttrs != null) {
                    StringBuilder sb = new StringBuilder();
                    for (ConfDataThresholdQualifyingAttr perThresholdQualifyingAttr : perThresholdQualifyingAttrs) {
                        sb.append(String.format("%s->%s$", perThresholdQualifyingAttr.getAttributeType(), perThresholdQualifyingAttr.getAttributeValue()));
                    }
                    config.setExtendedQualifyingAttributes(sb.toString());
                }
            }

            return thresholdConfigs;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            return new ArrayList<ThresholdConfigExtendedForDisplay>();
        }
    }

    private Map<Long, ConfDataAlertingConfig> getAlertingConfigTableData() {

        HashMap<Long, ConfDataAlertingConfig> configMap = new HashMap<Long, ConfDataAlertingConfig>();
        try  {
            List<ConfDataAlertingConfig> alertingConfigs;

            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            alertingConfigs = confDataDAO.selectAll(ConfDataAlertingConfig.TYPE_NAME, ConfDataAlertingConfig.class);


            for (ConfDataAlertingConfig config: alertingConfigs) {
                configMap.put(config.getId(),config);
            }

            return configMap;
        }
        catch(ConfDataDAOException cddEx)  {
            error(cddEx);

            configMap.clear();
            return configMap;
        }
    }

    private Map<Long, List<ConfDataThresholdContextAttr>> getThresholdContextAttrTableData() {


        HashMap<Long, List<ConfDataThresholdContextAttr>> configMap = new HashMap<Long, List<ConfDataThresholdContextAttr>>();
        try {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataThresholdContextAttr> contextAttrs;
            contextAttrs = confDataDAO.selectAll(ConfDataThresholdContextAttr.TYPE_NAME, ConfDataThresholdContextAttr.class);

            // add in all our context attr's, per threshold
            for (ConfDataThresholdContextAttr contextAttr : contextAttrs) {

                List<ConfDataThresholdContextAttr> perThresholdContextAttrs = configMap.get(contextAttr.getThresholdConfigId());
                if (perThresholdContextAttrs == null) {
                    perThresholdContextAttrs = new ArrayList<ConfDataThresholdContextAttr>();
                    configMap.put(contextAttr.getThresholdConfigId(), perThresholdContextAttrs);
                }
                perThresholdContextAttrs.add(contextAttr);
            }

            // sort each list per attribute
            for (List<ConfDataThresholdContextAttr> perThresholdContextAttrs : configMap.values()) {
                Collections.sort(perThresholdContextAttrs, ThresholdContextAttrByAttributeTypeComparator.getInstance());
            }

            return configMap;
        }
        catch (ConfDataDAOException cddEx) {
            error(cddEx);

            configMap.clear();
            return configMap;
        }
    }

    private Map<Long, List<ConfDataThresholdQualifyingAttr>> getThresholdQualifyingAttrTableData() {

        HashMap<Long, List<ConfDataThresholdQualifyingAttr>> configMap = new HashMap<Long, List<ConfDataThresholdQualifyingAttr>>();
        try {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

            List<ConfDataThresholdQualifyingAttr> qualifyingAttrs;
            qualifyingAttrs = confDataDAO.selectAll(ConfDataThresholdQualifyingAttr.TYPE_NAME, ConfDataThresholdQualifyingAttr.class);

            // add in all our qualifying attr's, per threshold
            for (ConfDataThresholdQualifyingAttr qualifyingAttr : qualifyingAttrs) {

                List<ConfDataThresholdQualifyingAttr> perThresholdQualifyingAttrs = configMap.get(qualifyingAttr.getThresholdConfigId());
                if (perThresholdQualifyingAttrs == null) {
                    perThresholdQualifyingAttrs = new ArrayList<ConfDataThresholdQualifyingAttr>();
                    configMap.put(qualifyingAttr.getThresholdConfigId(), perThresholdQualifyingAttrs);
                }
                perThresholdQualifyingAttrs.add(qualifyingAttr);
            }

            // sort each list per attribute/value
            for (List<ConfDataThresholdQualifyingAttr> perThresholdQualifyingAttrs : configMap.values()) {
                Collections.sort(perThresholdQualifyingAttrs, ThresholdQualifyingAttrByAttributeTypeComparator.getInstance());
            }

            return configMap;
        }
        catch (ConfDataDAOException cddEx) {
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

    private class ModalWindowWithThreshold extends ModalWindow {
        private volatile ConfDataThresholdConfig threshold = null;

        public ModalWindowWithThreshold(String id) {
            super(id);

            setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

                @Override
                public void onClose(AjaxRequestTarget target) {

                    if (infoFeedbackMessage != null) {
                        info(infoFeedbackMessage);

                        Map<Long, ConfDataAlertingConfig> newAlertConfigs = getAlertingConfigTableData();
                        alertingConfigs.clear();
                        alertingConfigs.putAll(newAlertConfigs);

                        Map<Long, List<ConfDataThresholdContextAttr>> newContextAttrs = getThresholdContextAttrTableData();
                        contextAttrs.clear();
                        contextAttrs.putAll(newContextAttrs);

                        Map<Long, List<ConfDataThresholdQualifyingAttr>> newQualifyingAttrs = getThresholdQualifyingAttrTableData();
                        qualifyingAttrs.clear();
                        qualifyingAttrs.putAll(newQualifyingAttrs);

                        List<ThresholdConfigExtendedForDisplay> updatedThresholds = getThresholdConfigTableData(alertingConfigs,contextAttrs,qualifyingAttrs);
                        dataProvider.updateDataList(updatedThresholds);
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

        public void setThreshold(ConfDataThresholdConfig threshold) {
            this.threshold = threshold;
        }
    }

    private class ActionPanel extends Panel {

        private ConfDataThresholdConfig selected;

        public ActionPanel(String id, IModel<ThresholdConfigExtendedForDisplay> model) {
            super(id, model);
            add(new AjaxFallbackLink("edit") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (ConfDataThresholdConfig) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    editThresholdModalWindow.setThreshold(selected);
                    editThresholdModalWindow.show(target);
                }
            });

            add(new AjaxFallbackLink("delete") {
                @Override
                public void onClick(AjaxRequestTarget target) {

                    selected = (ConfDataThresholdConfig) getParent().getDefaultModelObject();

                    Session.get().getFeedbackMessages().clear();
                    target.addComponent(feedback);

                    deleteThresholdModalWindow.setThreshold(selected);
                    deleteThresholdModalWindow.show(target);
                }
            });
        }
    }

    private class OptionsPanel extends Panel {

        public OptionsPanel(String id, final IModel<ThresholdConfigExtendedForDisplay> model) {
            super(id, model);

            ConfDataThresholdConfig config = (ConfDataThresholdConfig) model.getObject();

            final List<String> optionNames = new ArrayList<String>();
            final List<String> optionValues = new ArrayList<String>();

            if(config.getMinThresholdValue() != null) {
                optionNames.add("min: ");
                optionValues.add(config.getMinThresholdValue().toString());
            }

            if(config.getMaxThresholdValue() != null) {
                optionNames.add("max: ");
                optionValues.add(config.getMaxThresholdValue().toString());
            }

            if(config.getMinThresholdSamples() != null) {
                optionNames.add("samples: ");
                optionValues.add(config.getMinThresholdSamples().toString());
            }

            if(config.getMaxSampleWindowMs() != null) {
                optionNames.add("sample window: ");
                optionValues.add(config.getMaxSampleWindowMs().toString() + " ms");
            }

            if(config.getClearingIntervalMs() != null) {
                optionNames.add("clearing interval: ");
                optionValues.add(config.getClearingIntervalMs().toString() + " ms");
            }

            add(new ListView<String>("options",optionNames) {

                @Override
                public void populateItem(final ListItem item) {
                    item.add(new Label("optionName",optionNames.get(item.getIndex())));
                    item.add(new Label("optionValue",optionValues.get(item.getIndex())));
                }
            });
        }
    }

    private class ThresholdContextAttrsPanel extends Panel {

        private ConfDataThresholdConfig selected;

        public ThresholdContextAttrsPanel(String id, final IModel<ThresholdConfigExtendedForDisplay> model) {
            super(id, model);

            ThresholdConfigExtendedForDisplay thresholdConfig = (ThresholdConfigExtendedForDisplay) model.getObject();
            List<ConfDataThresholdContextAttr> perThresholdContextAttrs = contextAttrs.get(thresholdConfig.getId());

            if(perThresholdContextAttrs ==  null) {
                perThresholdContextAttrs = new ArrayList<ConfDataThresholdContextAttr>();
            }

            add(new ListView<ConfDataThresholdContextAttr>("contextAttrs",perThresholdContextAttrs) {

                @Override
                public void populateItem(final ListItem item) {
                    ConfDataThresholdContextAttr contextAttr = (ConfDataThresholdContextAttr)item.getModelObject();
                    item.add(new Label("contextAttr",contextAttr.getAttributeType()));
                }
            });
        }
    }

    private class ThresholdQualifyingAttrsPanel extends Panel {

        private ConfDataThresholdConfig selected;

        public ThresholdQualifyingAttrsPanel(String id, final IModel<ThresholdConfigExtendedForDisplay> model) {
            super(id, model);

            ThresholdConfigExtendedForDisplay thresholdConfig = (ThresholdConfigExtendedForDisplay) model.getObject();
            List<ConfDataThresholdQualifyingAttr> perThresholdQualifyingAttrs = qualifyingAttrs.get(thresholdConfig.getId());

            if(perThresholdQualifyingAttrs ==  null) {
                perThresholdQualifyingAttrs = new ArrayList<ConfDataThresholdQualifyingAttr>();
            }

            add(new ListView<ConfDataThresholdQualifyingAttr>("qualifyingAttrs",perThresholdQualifyingAttrs) {

                @Override
                public void populateItem(final ListItem item) {
                    ConfDataThresholdQualifyingAttr qualifyingAttr = (ConfDataThresholdQualifyingAttr)item.getModelObject();
                    item.add(new Label("qualifyingAttr",qualifyingAttr.getAttributeType() + "->" + qualifyingAttr.getAttributeValue()));
                }
            });
        }
    }
}
