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

package com.ning.arecibo.alertmanager.tabs.alertingconfigs;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class AlertingConfigDeleteFormPanel extends Panel {
    final static Logger log = Logger.getLogger(AlertingConfigDeleteFormPanel.class);

    public AlertingConfigDeleteFormPanel(final String name,final FeedbackPanel feedback,final ModalWindow window,
                                         final AlertingConfigsPanel parentPanel, final AlertingConfigFormModel alertingConfigFormModel,final ModalMode mode) {

        super(name);

        boolean formEnabled = true;

        if(mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(DELETE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (alertingConfigFormModel.initNotificationGroupList(confDataDAO) &&
                                        alertingConfigFormModel.initManagingKeyList(confDataDAO));
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<AlertingConfigFormModel> form = new Form<AlertingConfigFormModel>("deleteForm",new CompoundPropertyModel<AlertingConfigFormModel>(alertingConfigFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!formEnabled) {
            feedback.error("Failed to load data db, cannot delete alert config at this time");
            form.setEnabled(false);
        }

        final Label alertingConfigName = new Label("alertingConfigName");
        form.add(alertingConfigName);

        final Label notifRepeatMode = new Label("notifRepeatMode");
        form.add(notifRepeatMode);

        final Label notifRepeatIntervalMs = new Label("notifRepeatIntervalMs");
        form.add(notifRepeatIntervalMs);

        final Label notifOnRecovery = new Label("notifOnRecovery");
        form.add(notifOnRecovery);

        final Label enabled = new Label("enabled");
        form.add(enabled);

        final ListView<ConfDataNotifGroup> notificationGroupList =
                new ListView<ConfDataNotifGroup>("notificationGroupList", alertingConfigFormModel.getNotificationGroupList()) {


                    @Override
                    public void populateItem(final ListItem item) {

                        final ConfDataNotifGroup group = (ConfDataNotifGroup) item.getModelObject();

                        final Label notificationGroup = new Label("notificationGroup",group.getLabel());
                        item.add(notificationGroup);
                    }
                };
        notificationGroupList.setOutputMarkupId(true);
        form.add(notificationGroupList);


        final ListView<ConfDataManagingKey> managingKeyList =
                new ListView<ConfDataManagingKey>("managingKeyList", alertingConfigFormModel.getManagingKeyList()) {

                    @Override
                    public void populateItem(final ListItem item) {

                        final ConfDataManagingKey managingKey = (ConfDataManagingKey) item.getModelObject();

                        final Label key = new Label("managingKey", managingKey.getKey());
                        item.add(key);
                    }
                };
        managingKeyList.setOutputMarkupId(true);
        form.add(managingKeyList);


        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("deleteButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                AlertingConfigFormModel alertingConfigFormModel = (AlertingConfigFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;

                    if(mode.equals(DELETE)) {
                        success = alertingConfigFormModel.delete(confDataDAO);
                    }
                    else{
                        throw new IllegalStateException();
                    }

                    String statusMessage = alertingConfigFormModel.getLastStatusMessage();

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
