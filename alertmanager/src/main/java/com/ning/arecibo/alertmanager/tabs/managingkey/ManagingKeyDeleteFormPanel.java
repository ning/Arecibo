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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.converters.ZeroPaddingIntegerConverter;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

import com.ning.arecibo.util.Logger;

public class ManagingKeyDeleteFormPanel extends Panel {
    final static Logger log = Logger.getLogger(ManagingKeyDeleteFormPanel.class);

    private static final IConverter MINUTES_CONVERTER = new ZeroPaddingIntegerConverter(2);

    public ManagingKeyDeleteFormPanel(String name, final FeedbackPanel feedback, final ModalWindow window, final ManagingKeyPanel parentPanel,
                                      final ManagingKeyFormModel managingKeyFormModel,final ModalMode mode) {
        super(name);

        final Form<ManagingKeyFormModel> form = new Form<ManagingKeyFormModel>("deleteForm",new CompoundPropertyModel<ManagingKeyFormModel>(managingKeyFormModel));
        add(form);

        if (mode == null) {
            throw new IllegalStateException();
        }
        else if (mode.equals(DELETE)) {
            form.setOutputMarkupId(true);

            final Label key = new Label("key");
            form.add(key);

            final Label action = new Label("action");
            form.add(action);

            final Label manualOverrideUntil = new Label("manualOverrideUntilString");
            form.add(manualOverrideUntil);

            final Label manualOverrideIndefinitely = new Label("manualOverrideIndefinitely");
            form.add(manualOverrideIndefinitely);

            final Label autoActivateTODStartHours = new Label("autoActivateTODStartHours") {
                @Override
                public IConverter getConverter(Class type) {
                    return MINUTES_CONVERTER;
                }
            };
            form.add(autoActivateTODStartHours);

            final Label autoActivateTODStartMinutes = new Label("autoActivateTODStartMinutes") {
                @Override
                public IConverter getConverter
                        (Class type) {
                    return MINUTES_CONVERTER;
                }
            };
            form.add(autoActivateTODStartMinutes);

            final Label autoActivateTODEndHours = new Label("autoActivateTODEndHours") {
                @Override
                public IConverter getConverter(Class type) {
                    return MINUTES_CONVERTER;
                }
            };
            form.add(autoActivateTODEndHours);

            final Label autoActivateTODEndMinutes = new Label("autoActivateTODEndMinutes") {
                @Override
                public IConverter getConverter(Class type) {
                    return MINUTES_CONVERTER;
                }
            };
            form.add(autoActivateTODEndMinutes);


            final Label autoActivateDOWStart = new Label("autoActivateDOWStart", new PropertyModel<String>(managingKeyFormModel, "autoActivateDOWStartString"));
            form.add(autoActivateDOWStart);

            final Label autoActivateDOWEnd = new Label("autoActivateDOWEnd", new PropertyModel<String>(managingKeyFormModel, "autoActivateDOWEndString"));
            form.add(autoActivateDOWEnd);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

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

                    if (mode.equals(DELETE)) {
                        success = managingKey.delete(confDataDAO);
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
