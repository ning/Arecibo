package com.ning.arecibo.alertmanager.tabs.alertincidents;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilteredAbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterForm;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.GoAndClearFilter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.FilterToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.tabs.LazyLoadingWaitPanel;
import com.ning.arecibo.alertmanager.utils.SortableConfDataProvider;
import com.ning.arecibo.alertmanager.utils.columns.PropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TextFilteredPropertyColumnWithCssClass;
import com.ning.arecibo.alertmanager.utils.columns.TimestampPropertyColumn;
import com.ning.arecibo.alertmanager.utils.comparators.ConfDataObjectByUpdateTimestampComparator;

import com.ning.arecibo.util.Logger;



public class AlertIncidentLogsPanel extends Panel{
    final static Logger log = Logger.getLogger(AlertIncidentLogsPanel.class);

    final FeedbackPanel feedback;
    final AlertIncidentSearchRangeModel searchRangeModel;
    final Form<AlertIncidentSearchRangeModel> searchRangeForm;
    //final Panel tablePanel;

    private volatile String infoFeedbackMessage = null;
    private volatile String errorFeedbackMessage = null;

    public AlertIncidentLogsPanel(String id) {
        super(id);
        setOutputMarkupId(true);

        searchRangeModel = new AlertIncidentSearchRangeModel();
        searchRangeForm = getSearchRangeForm(searchRangeModel);
        add(searchRangeForm);

        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        Panel tablePanel = getTablePanel("alertIncidentLogTablePanel");
        add(tablePanel);
    }

    private Panel getTablePanel(String panelId) {

        return new AjaxLazyLoadPanel(panelId) {

            @Override
            public Component getLazyLoadComponent(String panelId) {
                AlertIncidentLogTablePanel tablePanel = new AlertIncidentLogTablePanel(panelId,searchRangeModel);
                return tablePanel;
            }

            @Override
            public Component getLoadingComponent(String panelId) {
                return new LazyLoadingWaitPanel(panelId);
            }
        };
    }

    private Form<AlertIncidentSearchRangeModel> getSearchRangeForm(final AlertIncidentSearchRangeModel searchRangeModel) {

        Form<AlertIncidentSearchRangeModel> searchRangeForm=new Form<AlertIncidentSearchRangeModel>("searchRangeForm",
                new CompoundPropertyModel<AlertIncidentSearchRangeModel>(searchRangeModel));

        add(searchRangeForm);

        final DateTimeField searchRangeStart = new DateTimeField("searchRangeStart") {
            @Override
            protected boolean use12HourFormat() {
                // use 24 hour format
                return false;
            }
        };
        //searchRangeStart.add(DateValidator.maximum(new Date(System.currentTimeMillis())));
        searchRangeStart.setRequired(true);
        searchRangeForm.add(searchRangeStart);

        final DateTimeField searchRangeEnd = new DateTimeField("searchRangeEnd") {
            @Override
            protected boolean use12HourFormat() {
                // use 24 hour format
                return false;
            }
        };
        //searchRangeEnd.add(DateValidator.maximum(new Date(System.currentTimeMillis())));
        searchRangeEnd.setRequired(true);
        searchRangeForm.add(searchRangeEnd);


        searchRangeForm.add(new AjaxFallbackButton("updateSearchButton", searchRangeForm) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                try {

                    // validate date range
                    if(!searchRangeModel.validate()) {
                        error(searchRangeModel.getLastValidationMessage());
                    }
                    else {
                        Panel tablePanel = getTablePanel("alertIncidentLogTablePanel");

                        AlertIncidentLogsPanel.this.replace(tablePanel);
                        target.addComponent(AlertIncidentLogsPanel.this);
                    }
                }
                catch(Exception ex) {
                    log.warn(ex,"Exception updating search results");
                    error(ex);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {

                // repaint the feedback panel
                target.addComponent(feedback);
            }
        });


        return searchRangeForm;
    }
}
