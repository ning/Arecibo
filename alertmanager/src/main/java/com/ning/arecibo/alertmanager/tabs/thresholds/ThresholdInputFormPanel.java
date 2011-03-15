package com.ning.arecibo.alertmanager.tabs.thresholds;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.Component;
import org.apache.wicket.validation.validator.MinimumValidator;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertingConfig;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdContextAttr;
import com.ning.arecibo.alert.confdata.objects.ConfDataThresholdQualifyingAttr;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ThresholdInputFormPanel extends Panel {
    final static Logger log = Logger.getLogger(ThresholdInputFormPanel.class);

    public ThresholdInputFormPanel(final String name,final FeedbackPanel feedback,final ModalWindow window,
                                   final ThresholdsPanel parentPanel, final ThresholdFormModel thresholdFormModel, final ModalMode mode) {

        super(name);

        boolean formEnabled = true;

        if(mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(INSERT)) {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = thresholdFormModel.initAlertConfigListChoices(confDataDAO);
        }
        else if(mode.equals(UPDATE)) {
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (thresholdFormModel.initAlertConfigListChoices(confDataDAO) &&
                           thresholdFormModel.initQualifyingAttrList(confDataDAO) &&
                           thresholdFormModel.initContextAttrList(confDataDAO));
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<ThresholdFormModel> form = new Form<ThresholdFormModel>("inputForm",new CompoundPropertyModel<ThresholdFormModel>(thresholdFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!formEnabled) {
            feedback.error("Failed to load alert config choices from the db, cannot create threshold at this time");
            form.setEnabled(false);
        }

        final RequiredTextField<String> thresholdName = new RequiredTextField<String>("thresholdName");
        form.add(thresholdName);

        final RequiredTextField<String> monitoredEventType = new RequiredTextField<String>("monitoredEventType");
        form.add(monitoredEventType);

        final RequiredTextField<String> monitoredAttributeType = new RequiredTextField<String>("monitoredAttributeType");
        form.add(monitoredAttributeType);

        final TextField<Double> minThresholdValue = new TextField<Double>("minThresholdValue");
        form.add(minThresholdValue);

        final TextField<Double> maxThresholdValue = new TextField<Double>("maxThresholdValue");
        form.add(maxThresholdValue);

        final RequiredTextField<Long> minThresholdSamples = new RequiredTextField<Long>("minThresholdSamples");
        minThresholdSamples.add(new MinimumValidator<Long>(1L));
        form.add(minThresholdSamples);

        final TextField<Long> maxSampleWindowMs = new TextField<Long>("maxSampleWindowMs");
        maxSampleWindowMs.add(new MinimumValidator<Long>(0L));
        form.add(maxSampleWindowMs);

        final RequiredTextField<Long> clearingIntervalMs = new RequiredTextField<Long>("clearingIntervalMs");
        clearingIntervalMs.add(new MinimumValidator<Long>(1L));
        form.add(clearingIntervalMs);

        final WebMarkupContainer qualifyingAttrListContainer = new WebMarkupContainer("qualifyingAttrListContainer");
        qualifyingAttrListContainer.setOutputMarkupId(true);
        form.add(qualifyingAttrListContainer);

        final ListView<ConfDataThresholdQualifyingAttr> qualifyingAttrList =
                new ListView<ConfDataThresholdQualifyingAttr>("qualifyingAttrList", thresholdFormModel.getQualifyingAttrList()) {

                    private Component hiddenRemoveButton = null;

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataThresholdQualifyingAttr> thisListView = this;

                        final ConfDataThresholdQualifyingAttr attr = (ConfDataThresholdQualifyingAttr) item.getModelObject();

                        // attributeType field
                        final RequiredTextField<String> attributeType = new RequiredTextField<String>("qualifyingAttributeType",
                                new PropertyModel<String>(attr, "attributeType"));
                        attributeType.setOutputMarkupId(true);
                        attributeType.add(new AjaxFormValidatingBehavior(form, "onchange") {

                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                attr.setAttributeType(attributeType.getInput());
                            }
                        });
                        item.add(attributeType);

                        // attributeValue field
                        final RequiredTextField<String> attributeValue = new RequiredTextField<String>("qualifyingAttributeValue",
                                new PropertyModel<String>(attr, "attributeValue"));
                        attributeValue.setOutputMarkupId(true);
                        attributeValue.add(new AjaxFormValidatingBehavior(form, "onchange") {

                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                attr.setAttributeValue(attributeValue.getInput());
                            }
                        });
                        item.add(attributeValue);

                        // remove button
                        AjaxFallbackButton removeQualifyingAttributeButton =
                                new AjaxFallbackButton("removeQualifyingAttribute",form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        thresholdFormModel.getQualifyingAttrList().remove(item.getIndex());
                                        thisListView.modelChanged();
                                        target.addComponent(qualifyingAttrListContainer);
                                    }
                                };

                        removeQualifyingAttributeButton.setDefaultFormProcessing(false);
                        removeQualifyingAttributeButton.setOutputMarkupPlaceholderTag(true);
                        item.add(removeQualifyingAttributeButton);
                    }
                };
        qualifyingAttrList.setOutputMarkupId(true);
        qualifyingAttrListContainer.add(qualifyingAttrList);


        final AjaxFallbackButton addQualifyingAttrButton =
                new AjaxFallbackButton("addQualifyingAttribute", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        thresholdFormModel.getQualifyingAttrList().add(new ConfDataThresholdQualifyingAttr());
                        qualifyingAttrList.modelChanged();
                        target.addComponent(qualifyingAttrListContainer);
                    }
                };
        addQualifyingAttrButton.setDefaultFormProcessing(false);
        form.add(addQualifyingAttrButton);


        final WebMarkupContainer contextAttrListContainer = new WebMarkupContainer("contextAttrListContainer");
        contextAttrListContainer.setOutputMarkupId(true);
        form.add(contextAttrListContainer);

        final ListView<ConfDataThresholdContextAttr> contextAttrList =
                new ListView<ConfDataThresholdContextAttr>("contextAttrList", thresholdFormModel.getContextAttrList()) {

                    private Component hiddenRemoveButton = null;

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataThresholdContextAttr> thisListView = this;

                        final ConfDataThresholdContextAttr attr = (ConfDataThresholdContextAttr) item.getModelObject();

                        // attributeType field
                        final RequiredTextField<String> attributeType = new RequiredTextField<String>("contextAttributeType",
                                new PropertyModel<String>(attr, "attributeType"));
                        attributeType.setOutputMarkupId(true);
                        attributeType.add(new AjaxFormValidatingBehavior(form, "onchange") {

                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                attr.setAttributeType(attributeType.getInput());
                            }
                        });
                        item.add(attributeType);

                        // remove button
                        AjaxFallbackButton removeContextAttributeButton =
                                new AjaxFallbackButton("removeContextAttribute",form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        thresholdFormModel.getContextAttrList().remove(item.getIndex());
                                        thisListView.modelChanged();
                                        target.addComponent(contextAttrListContainer);
                                    }
                                };

                        removeContextAttributeButton.setDefaultFormProcessing(false);
                        removeContextAttributeButton.setOutputMarkupPlaceholderTag(true);
                        item.add(removeContextAttributeButton);
                    }
                };
        contextAttrList.setOutputMarkupId(true);
        contextAttrListContainer.add(contextAttrList);


        final AjaxFallbackButton addContextAttrButton =
                new AjaxFallbackButton("addContextAttribute", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        thresholdFormModel.getContextAttrList().add(new ConfDataThresholdContextAttr());
                        contextAttrList.modelChanged();
                        target.addComponent(contextAttrListContainer);
                    }
                };
        addContextAttrButton.setDefaultFormProcessing(false);
        form.add(addContextAttrButton);


        final DropDownChoice<String> alertingConfig = new DropDownChoice<String>("alertingConfigName",
                new Model<String>(thresholdFormModel.getAlertingConfigName(thresholdFormModel.getAlertingConfigId())),
                thresholdFormModel.getAlertingConfigList());

        alertingConfig.setRequired(true);
        alertingConfig.setOutputMarkupId(true);
        alertingConfig.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String selectedAlertConfigName = alertingConfig.getModelObject();
                ConfDataAlertingConfig selectedConfig = thresholdFormModel.getAlertingConfigFromAlertingConfigName(selectedAlertConfigName);
                thresholdFormModel.setAlertingConfigId(selectedConfig.getId());
                target.addComponent(alertingConfig);
            }
        });
        form.add(alertingConfig);


        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("saveButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                ThresholdFormModel threshold = (ThresholdFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;

                    if(mode.equals(INSERT)) {
                        success = threshold.insert(confDataDAO);
                    }
                    else if(mode.equals(UPDATE)) {
                        success = threshold.update(confDataDAO);
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
