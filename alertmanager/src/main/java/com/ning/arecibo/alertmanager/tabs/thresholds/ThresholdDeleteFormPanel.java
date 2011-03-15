package com.ning.arecibo.alertmanager.tabs.thresholds;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ThresholdDeleteFormPanel extends Panel {
    final static Logger log = Logger.getLogger(ThresholdDeleteFormPanel.class);

    public ThresholdDeleteFormPanel(final String name,final FeedbackPanel feedback,final ModalWindow window,final ThresholdsPanel parentPanel,
                                   final ThresholdFormModel thresholdFormModel,final ModalMode mode) {

        super(name);

        boolean formEnabled = true;

        if(mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(DELETE)) {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (thresholdFormModel.initAlertConfigListChoices(confDataDAO) &&
                            thresholdFormModel.initQualifyingAttrList(confDataDAO) &&
                            thresholdFormModel.initContextAttrList(confDataDAO));
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<ThresholdFormModel> form = new Form<ThresholdFormModel>("deleteForm",new CompoundPropertyModel<ThresholdFormModel>(thresholdFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!formEnabled) {
            feedback.error("Failed to load alert config choices from the db, cannot delete threshold at this time");
            form.setEnabled(false);
        }

        final Label thresholdName = new Label("thresholdName");
        form.add(thresholdName);

        final Label monitoredEventType = new Label("monitoredEventType");
        form.add(monitoredEventType);

        final Label monitoredAttributeType = new Label("monitoredAttributeType");
        form.add(monitoredAttributeType);

        final Label minThresholdValue = new Label("minThresholdValue");
        form.add(minThresholdValue);

        final Label maxThresholdValue = new Label("maxThresholdValue");
        form.add(maxThresholdValue);

        final Label minThresholdSamples = new Label("minThresholdSamples");
        form.add(minThresholdSamples);

        final Label maxSampleWindowMs = new Label("maxSampleWindowMs");
        form.add(maxSampleWindowMs);

        final Label clearingIntervalMs = new Label("clearingIntervalMs");
        form.add(clearingIntervalMs);

        final ListView<ConfDataThresholdQualifyingAttr> qualifyingAttrList =
                new ListView<ConfDataThresholdQualifyingAttr>("qualifyingAttrList", thresholdFormModel.getQualifyingAttrList()) {

                    @Override
                    public void populateItem(final ListItem item) {

                        final ConfDataThresholdQualifyingAttr attr = (ConfDataThresholdQualifyingAttr) item.getModelObject();

                        // attributeType field
                        final Label attributeType = new Label("qualifyingAttributeType",new PropertyModel<String>(attr, "attributeType"));
                        item.add(attributeType);

                        // attributeValue field
                        final Label attributeValue = new Label("qualifyingAttributeValue",new PropertyModel<String>(attr, "attributeValue"));
                        item.add(attributeValue);
                    }
                };
        qualifyingAttrList.setOutputMarkupId(true);
        form.add(qualifyingAttrList);

        final ListView<ConfDataThresholdContextAttr> contextAttrList =
                new ListView<ConfDataThresholdContextAttr>("contextAttrList", thresholdFormModel.getContextAttrList()) {

                    @Override
                    public void populateItem(final ListItem item) {

                        final ConfDataThresholdContextAttr attr = (ConfDataThresholdContextAttr) item.getModelObject();

                        // attributeType field
                        final Label attributeType = new Label("contextAttributeType", new PropertyModel<String>(attr, "attributeType"));
                        item.add(attributeType);
                    }
                };
        contextAttrList.setOutputMarkupId(true);
        form.add(contextAttrList);


        final Label alertConfig = new Label("alertConfigName",new Model<String>(thresholdFormModel.getAlertingConfigName(thresholdFormModel.getAlertingConfigId())));
        form.add(alertConfig);


        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("deleteButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                ThresholdFormModel threshold = (ThresholdFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;

                    if(mode.equals(DELETE)) {
                        success = threshold.delete(confDataDAO);
                    }
                    else {
                        throw new IllegalStateException("Unrecognized mode: " + mode);
                    }


                    String statusMessage = threshold.getLastStatusMessage();

                    if (success) {
                        window.close(target);
                        parentPanel.sendInfoFeedback(statusMessage);
                    }
                    else {
                        error(statusMessage);

                        // show warning
                        warningLabel.setVisible(true);
                        target.addComponent(warningLabel);
                    }

                    log.info(statusMessage);

                }
                catch (Exception e) {
                    log.warn(e);
                    error(e);

                    // show warning
                    warningLabel.setVisible(true);
                    target.addComponent(warningLabel);
                }

                // repaint the feedback panel
                target.addComponent(feedback);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {

                // show warning
                warningLabel.setVisible(true);
                target.addComponent(warningLabel);

                // repaint the feedback panel
                target.addComponent(feedback);
            }
        });

        AjaxFallbackButton cancelButton = new AjaxFallbackButton("cancelButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                window.close(target);
            }
        };
        cancelButton.setDefaultFormProcessing(false);
        form.add(cancelButton);
    }
}
