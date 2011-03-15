package com.ning.arecibo.alertmanager.tabs.managingkey;

import java.util.Arrays;
import java.util.Date;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.DateValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converters.ZeroPaddingIntegerConverter;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.enums.ManagingKeyActionType;
import com.ning.arecibo.alertmanager.AreciboAlertManager;
import com.ning.arecibo.alertmanager.utils.DayOfWeekUtils;
import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ManagingKeyInputFormPanel extends Panel {
    final static Logger log = Logger.getLogger(ManagingKeyInputFormPanel.class);

    private final static IConverter MINUTES_CONVERTER = new ZeroPaddingIntegerConverter(2);

    public ManagingKeyInputFormPanel(String name,final FeedbackPanel feedback, final ModalWindow window, final ManagingKeyPanel parentPanel,
                                     final ManagingKeyFormModel managingKeyFormModel,final ModalMode mode) {
        super(name);

        if (mode == null) {
            throw new IllegalStateException();
        }
        else if (!mode.equals(INSERT) && !mode.equals(UPDATE)) {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<ManagingKeyFormModel> form = new Form<ManagingKeyFormModel>("inputForm",new CompoundPropertyModel<ManagingKeyFormModel>(managingKeyFormModel));
        form.setOutputMarkupId(true);
        add(form);

        
        final RequiredTextField<String> key = new RequiredTextField<String>("key");
        form.add(key);


        final DropDownChoice<ManagingKeyActionType> action = new DropDownChoice<ManagingKeyActionType>("action",
                new PropertyModel<ManagingKeyActionType>(managingKeyFormModel, "action"),
                Arrays.asList(ManagingKeyActionType.values()));
        action.setRequired(true);
        form.add(action);

        final DateTimeField manualOverrideUntil = new DateTimeField("manualOverrideUntil") {
            @Override
            protected boolean use12HourFormat() {
                // use 24 hour format
                return false;
            }
        };
        manualOverrideUntil.setOutputMarkupPlaceholderTag(true);
        manualOverrideUntil.add(DateValidator.minimum(new Date(System.currentTimeMillis())));
        form.add(manualOverrideUntil);


        final AjaxCheckBox manualOverrideIndefinitely = new AjaxCheckBox("manualOverrideIndefinitely") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {

                Boolean checked = this.getModelObject();

                manualOverrideUntil.setEnabled(!checked);
                target.addComponent(manualOverrideUntil);
            }
        };
        form.add(manualOverrideIndefinitely);

        if (managingKeyFormModel.getManualOverrideIndefinitely() != null &&
                managingKeyFormModel.getManualOverrideIndefinitely()) {
            manualOverrideUntil.setEnabled(false);
        }


        final TextField<Integer> autoActivateTODStartHours = new TextField<Integer>("autoActivateTODStartHours") {
            @Override
            public IConverter getConverter(Class type) {
                return MINUTES_CONVERTER;
            }
        };
        autoActivateTODStartHours.add(new RangeValidator<Integer>(0, 23));
        form.add(autoActivateTODStartHours);

        final TextField<Integer> autoActivateTODStartMinutes = new TextField<Integer>("autoActivateTODStartMinutes") {
            @Override
            public IConverter getConverter
                    (Class type) {
                return MINUTES_CONVERTER;
            }
        };
        autoActivateTODStartMinutes.add(new RangeValidator<Integer>(0, 59));
        form.add(autoActivateTODStartMinutes);

        final TextField<Integer> autoActivateTODEndHours = new TextField<Integer>("autoActivateTODEndHours") {
            @Override
            public IConverter getConverter(Class type) {
                return MINUTES_CONVERTER;
            }
        };

        autoActivateTODEndHours.add(new RangeValidator<Integer>(0, 23));
        form.add(autoActivateTODEndHours);

        final TextField<Integer> autoActivateTODEndMinutes = new TextField<Integer>("autoActivateTODEndMinutes") {
            @Override
            public IConverter getConverter(Class type) {
                return MINUTES_CONVERTER;
            }
        };
        autoActivateTODEndMinutes.add(new RangeValidator<Integer>(0, 59));
        form.add(autoActivateTODEndMinutes);


        final DropDownChoice<String> autoActivateDOWStart = new DropDownChoice<String>("autoActivateDOWStart",
                new PropertyModel<String>(managingKeyFormModel, "autoActivateDOWStartString"),
                Arrays.asList(DayOfWeekUtils.DaysOfTheWeekStrings));
        form.add(autoActivateDOWStart);

        final DropDownChoice<String> autoActivateDOWEnd = new DropDownChoice<String>("autoActivateDOWEnd",
                new PropertyModel<String>(managingKeyFormModel, "autoActivateDOWEndString"),
                Arrays.asList(DayOfWeekUtils.DaysOfTheWeekStrings));
        form.add(autoActivateDOWEnd);

        final Label warningLabel = new Label("submissionWarningMessage", "There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("saveButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                ManagingKeyFormModel managingKey = (ManagingKeyFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
                    boolean success;

                    if (mode.equals(INSERT)) {
                        success = managingKey.insert(confDataDAO);
                    }
                    else if (mode.equals(UPDATE)) {
                        success = managingKey.update(confDataDAO);
                    }
                    else {
                        throw new IllegalStateException("Unrecognized mode: " + mode);
                    }

                    String statusMessage = managingKey.getLastStatusMessage();

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
