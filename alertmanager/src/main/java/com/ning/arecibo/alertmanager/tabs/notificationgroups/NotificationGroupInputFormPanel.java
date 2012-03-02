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

package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.Component;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class NotificationGroupInputFormPanel extends Panel {
    final static Logger log = Logger.getLogger(NotificationGroupInputFormPanel.class);

    public NotificationGroupInputFormPanel(final String name,final FeedbackPanel feedback,final ModalWindow window,
                                           final NotificationGroupsPanel parentPanel, final NotificationGroupFormModel notifGroupFormModel,final ModalMode mode) {

        super(name);

        boolean formEnabled = true;

        if (mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(INSERT)) {
            // add an initial notification, effectively require at least one
            notifGroupFormModel.getNotificationConfigList().add(new ConfDataNotifConfig());

            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = notifGroupFormModel.initNotificationConfigListChoices(confDataDAO);
        }
        else if(mode.equals(UPDATE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            formEnabled = (notifGroupFormModel.initNotificationConfigList(confDataDAO) &&
                            notifGroupFormModel.initNotificationConfigListChoices(confDataDAO));
        }

        final Form<NotificationGroupFormModel> form = new Form<NotificationGroupFormModel>("inputForm",new CompoundPropertyModel<NotificationGroupFormModel>(notifGroupFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!formEnabled) {
            feedback.error("Failed to load email notification list from the db, cannot edit notification group at this time");
            form.setEnabled(false);
        }

        final RequiredTextField<String> groupName = new RequiredTextField<String>("groupName");
        form.add(groupName);

        final CheckBox enabled = new CheckBox("enabled");
        form.add(enabled);

        final WebMarkupContainer notificationConfigListContainer = new WebMarkupContainer("notificationConfigListContainer");
        notificationConfigListContainer.setOutputMarkupId(true);
        form.add(notificationConfigListContainer);

        final ListView<ConfDataNotifConfig> notificationConfigList =
                new ListView<ConfDataNotifConfig>("notificationConfigList", notifGroupFormModel.getNotificationConfigList()) {

                    private Component hiddenRemoveButton = null;

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataNotifConfig> thisList = this;

                        final ConfDataNotifConfig config = (ConfDataNotifConfig) item.getModelObject();

                        // email address, based on person chosen
                        final DropDownChoice<String> notificationAddress = new DropDownChoice<String>("notificationAddress",
                                new PropertyModel<String>(config,"address"),
                                notifGroupFormModel.getNotificationConfigAddressList(config.getPersonId()));

                        notificationAddress.setRequired(true);
                        notificationAddress.setOutputMarkupId(true);
                        notificationAddress.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                String selectedAddress = (String)notificationAddress.getModelObject();
                                ConfDataNotifConfig notificationConfig = notifGroupFormModel.getNotificationConfigByAddress(selectedAddress);
                                config.setPropertiesFromMap(notificationConfig.toPropertiesMap());
                            }
                        });
                        item.add(notificationAddress);

                        // person
                        final DropDownChoice<String> notificationPerson = new DropDownChoice<String>("notificationPerson",
                                new Model<String>(notifGroupFormModel.getPersonNickName(config.getPersonId())),
                                notifGroupFormModel.getPersonList());

                        notificationPerson.setRequired(true);
                        notificationPerson.setOutputMarkupId(true);
                        notificationPerson.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                String selectedPerson = notificationPerson.getModelObject();
                                List<String> choices = notifGroupFormModel.getNotificationConfigAddressList(selectedPerson);

                                // if only one in list, pre-populate, otherwise force user to choose
                                if (choices.size() == 1) {
                                    ConfDataNotifConfig notificationConfig = notifGroupFormModel.getNotificationConfigByAddress(choices.get(0));
                                    config.setPropertiesFromMap(notificationConfig.toPropertiesMap());
                                }
                                else
                                    config.setAddress(null);

                                notificationAddress.setChoices(choices);
                                notificationAddress.modelChanged();
                                target.addComponent(notificationAddress);
                            }
                        });
                        item.add(notificationPerson);

                        // remove button
                        AjaxFallbackButton removeNotificationConfigButton =
                                new AjaxFallbackButton("removeNotificationConfig",form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        notifGroupFormModel.getNotificationConfigList().remove(item.getIndex());
                                        thisList.modelChanged();
                                        target.addComponent(notificationConfigListContainer);
                                    }
                                };

                        if (notifGroupFormModel.getNotificationConfigList().size() <= 1) {
                            // require at least one email address
                            removeNotificationConfigButton.setVisible(false);
                            this.hiddenRemoveButton = removeNotificationConfigButton;
                        }
                        else {
                            removeNotificationConfigButton.setVisible(true);
                            if (this.hiddenRemoveButton != null) {
                                this.hiddenRemoveButton.setVisible(true);
                                this.hiddenRemoveButton = null;
                            }
                        }

                        removeNotificationConfigButton.setOutputMarkupPlaceholderTag(true);
                        removeNotificationConfigButton.setDefaultFormProcessing(false);
                        item.add(removeNotificationConfigButton);
                    }
                };
        notificationConfigList.setOutputMarkupId(true);
        notificationConfigListContainer.add(notificationConfigList);


        final AjaxFallbackButton addNotificationConfigButton =
                new AjaxFallbackButton("addNotificationConfig", form) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        notifGroupFormModel.getNotificationConfigList().add(new ConfDataNotifConfig());
                        notificationConfigList.modelChanged();
                        target.addComponent(notificationConfigListContainer);
                    }
                };
        addNotificationConfigButton.setDefaultFormProcessing(false);
        form.add(addNotificationConfigButton);

        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("saveButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                NotificationGroupFormModel notifGroup = (NotificationGroupFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;

                    if(mode == null) {
                        throw new IllegalStateException();
                    }
                    else if(mode.equals(INSERT)) {
                        success = notifGroup.insert(confDataDAO);
                    }
                    else if(mode.equals(UPDATE)){
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
