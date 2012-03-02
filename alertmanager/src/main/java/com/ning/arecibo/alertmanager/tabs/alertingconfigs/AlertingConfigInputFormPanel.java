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

import java.util.Arrays;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.Component;
import org.apache.wicket.validation.validator.MinimumValidator;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.enums.NotificationRepeatMode;
import com.ning.arecibo.alert.confdata.objects.ConfDataManagingKey;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifGroup;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class AlertingConfigInputFormPanel extends Panel {
    final static Logger log = Logger.getLogger(AlertingConfigInputFormPanel.class);

    public AlertingConfigInputFormPanel(final String name,final FeedbackPanel feedback,final ModalWindow window,
                                        final AlertingConfigsPanel parentPanel,final AlertingConfigFormModel alertingConfigFormModel,final ModalMode mode) {

        super(name);

        boolean formEnabled = false;

        if(mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(INSERT)) {
            // add an initial notification group, effectively require at least one
            alertingConfigFormModel.getNotificationGroupList().add(new ConfDataNotifGroup());

            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (alertingConfigFormModel.initNotificationGroupListChoices(confDataDAO) &&
                           alertingConfigFormModel.initManagingKeyListChoices(confDataDAO));
        }
        else if(mode.equals(UPDATE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (alertingConfigFormModel.initNotificationGroupListChoices(confDataDAO) &&
                            alertingConfigFormModel.initNotificationGroupList(confDataDAO) &&
                            alertingConfigFormModel.initManagingKeyListChoices(confDataDAO) &&
                            alertingConfigFormModel.initManagingKeyList(confDataDAO));
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<AlertingConfigFormModel> form = new Form<AlertingConfigFormModel>("inputForm",new CompoundPropertyModel<AlertingConfigFormModel>(alertingConfigFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!formEnabled) {
            feedback.error("Failed to load objects from the db, cannot create alerting config at this time");
            form.setEnabled(false);
        }

        final RequiredTextField<String> alertingConfigName = new RequiredTextField<String>("alertingConfigName");
        form.add(alertingConfigName);

        final RequiredTextField<Long> notifRepeatIntervalMs = new RequiredTextField<Long>("notifRepeatIntervalMs");
        notifRepeatIntervalMs.add(new MinimumValidator<Long>(0L));
        notifRepeatIntervalMs.setOutputMarkupPlaceholderTag(true);
        form.add(notifRepeatIntervalMs);

        final WebMarkupContainerWithAssociatedMarkup notifRepeatIntervalMsLabel = new WebMarkupContainerWithAssociatedMarkup("notifRepeatIntervalMsLabel");
        notifRepeatIntervalMsLabel.setOutputMarkupPlaceholderTag(true);
        form.add(notifRepeatIntervalMsLabel);

        if(alertingConfigFormModel.getNotifRepeatMode() == null || alertingConfigFormModel.getNotifRepeatMode().equals(NotificationRepeatMode.NO_REPEAT)) {
            notifRepeatIntervalMs.setVisible(false);
            notifRepeatIntervalMsLabel.setVisible(false);
        }

        final DropDownChoice<NotificationRepeatMode> notifRepeatMode = new DropDownChoice<NotificationRepeatMode>("notifRepeatMode",
                new PropertyModel<NotificationRepeatMode>(alertingConfigFormModel, "notifRepeatMode"),
                Arrays.asList(NotificationRepeatMode.values()));
        notifRepeatMode.setRequired(true);
        notifRepeatMode.setOutputMarkupId(true);
        notifRepeatMode.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                NotificationRepeatMode mode = notifRepeatMode.getModelObject();

                if(mode == null || mode.equals(NotificationRepeatMode.NO_REPEAT)) {
                    alertingConfigFormModel.setNotifRepeatIntervalMs(null);
                    notifRepeatIntervalMsLabel.setVisible(false);
                    notifRepeatIntervalMs.setVisible(false);
                    target.addComponent(notifRepeatIntervalMsLabel);
                    target.addComponent(notifRepeatIntervalMs);
                }
                else {
                    notifRepeatIntervalMsLabel.setVisible(true);
                    notifRepeatIntervalMs.setVisible(true);
                    target.addComponent(notifRepeatIntervalMsLabel);
                    target.addComponent(notifRepeatIntervalMs);
                }
            }
        });
        form.add(notifRepeatMode);

        final CheckBox notifOnRecovery = new CheckBox("notifOnRecovery");
        form.add(notifOnRecovery);

        final CheckBox enabled = new CheckBox("enabled");
        form.add(enabled);

        final WebMarkupContainer notificationGroupListContainer = new WebMarkupContainer("notificationGroupListContainer");
        notificationGroupListContainer.setOutputMarkupId(true);
        form.add(notificationGroupListContainer);

        final ListView<ConfDataNotifGroup> notificationGroupList =
                new ListView<ConfDataNotifGroup>("notificationGroupList", alertingConfigFormModel.getNotificationGroupList()) {

                    private Component hiddenRemoveButton = null;

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataNotifGroup> thisList = this;

                        final ConfDataNotifGroup group = (ConfDataNotifGroup) item.getModelObject();

                        final DropDownChoice<String> notificationGroup = new DropDownChoice<String>("notificationGroup",
                                new Model<String>(group.getLabel()),
                                alertingConfigFormModel.getNotificationGroupNameList());

                        notificationGroup.setRequired(true);
                        notificationGroup.setOutputMarkupId(true);
                        notificationGroup.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                String selectedGroupName = notificationGroup.getModelObject();
                                ConfDataNotifGroup selectedGroup = alertingConfigFormModel.getNotificationGroupByGroupName(selectedGroupName);
                                group.setPropertiesFromMap(selectedGroup.toPropertiesMap());
                                target.addComponent(notificationGroup);
                            }
                        });
                        item.add(notificationGroup);

                        // remove button
                        AjaxFallbackButton removeNotificationGroupButton =
                                new AjaxFallbackButton("removeNotificationGroup",form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        alertingConfigFormModel.getNotificationGroupList().remove(item.getIndex());
                                        thisList.modelChanged();
                                        target.addComponent(notificationGroupListContainer);
                                    }
                                };

                        if(alertingConfigFormModel.getNotificationGroupList().size() <= 1) {
                            // require at least one notif group
                            removeNotificationGroupButton.setVisible(false);
                            this.hiddenRemoveButton = removeNotificationGroupButton;
                        }
                        else {
                            removeNotificationGroupButton.setVisible(true);
                            if(this.hiddenRemoveButton != null) {
                                this.hiddenRemoveButton.setVisible(true);
                                this.hiddenRemoveButton = null;
                            }
                        }

                        removeNotificationGroupButton.setDefaultFormProcessing(false);
                        removeNotificationGroupButton.setOutputMarkupPlaceholderTag(true);
                        item.add(removeNotificationGroupButton);
                    }
                };
        notificationGroupList.setOutputMarkupId(true);
        notificationGroupListContainer.add(notificationGroupList);


        final AjaxFallbackButton addNotificationGroupButton =
                new AjaxFallbackButton("addNotificationGroup", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        alertingConfigFormModel.getNotificationGroupList().add(new ConfDataNotifGroup());
                        notificationGroupList.modelChanged();
                        target.addComponent(notificationGroupListContainer);
                    }
                };
        addNotificationGroupButton.setDefaultFormProcessing(false);
        form.add(addNotificationGroupButton);

        final WebMarkupContainer managingKeyListContainer = new WebMarkupContainer("managingKeyListContainer");
        managingKeyListContainer.setOutputMarkupId(true);
        form.add(managingKeyListContainer);

        final ListView<ConfDataManagingKey> managingKeyList =
                new ListView<ConfDataManagingKey>("managingKeyList", alertingConfigFormModel.getManagingKeyList()) {

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataManagingKey> thisList = this;

                        final ConfDataManagingKey key = (ConfDataManagingKey) item.getModelObject();

                        final DropDownChoice<String> managingKey = new DropDownChoice<String>("managingKey",
                                new Model<String>(key.getKey()),
                                alertingConfigFormModel.getManagingKeyNameList());

                        managingKey.setRequired(true);
                        managingKey.setOutputMarkupId(true);
                        managingKey.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                String selectedKeyName = managingKey.getModelObject();
                                ConfDataManagingKey selectedKey = alertingConfigFormModel.getManagingKeyByManagingKeyName(selectedKeyName);
                                key.setPropertiesFromMap(selectedKey.toPropertiesMap());
                                target.addComponent(managingKey);
                            }
                        });
                        item.add(managingKey);

                        // remove button
                        AjaxFallbackButton removeManagingKeyButton =
                                new AjaxFallbackButton("removeManagingKey", form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        alertingConfigFormModel.getManagingKeyList().remove(item.getIndex());
                                        thisList.modelChanged();
                                        target.addComponent(managingKeyListContainer);
                                    }
                                };

                        removeManagingKeyButton.setDefaultFormProcessing(false);
                        removeManagingKeyButton.setOutputMarkupPlaceholderTag(true);
                        item.add(removeManagingKeyButton);
                    }
                };
        managingKeyList.setOutputMarkupId(true);
        managingKeyListContainer.add(managingKeyList);


        final AjaxFallbackButton addManagingKeyButton =
                new AjaxFallbackButton("addManagingKey", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        alertingConfigFormModel.getManagingKeyList().add(new ConfDataManagingKey());
                        managingKeyList.modelChanged();
                        target.addComponent(managingKeyListContainer);
                    }
                };
        addManagingKeyButton.setDefaultFormProcessing(false);
        form.add(addManagingKeyButton);


        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("saveButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                AlertingConfigFormModel notifGroup = (AlertingConfigFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;

                    if(mode.equals(INSERT)) {
                        success = notifGroup.insert(confDataDAO);
                    }
                    else if(mode.equals(UPDATE)) {
                        success = notifGroup.update(confDataDAO);
                    }
                    else {
                        throw new IllegalStateException("Unrecognized mode: " + mode);
                    }


                    String statusMessage = notifGroup.getLastStatusMessage();

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
