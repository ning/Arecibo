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

package com.ning.arecibo.alertmanager.tabs.people;

import java.util.Arrays;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormValidatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.Component;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.enums.NotificationType;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class PersonInputFormPanel extends Panel {
    final static Logger log = Logger.getLogger(PersonInputFormPanel.class);

    public PersonInputFormPanel(String name,final FeedbackPanel feedback,final ModalWindow window,final PeoplePanel parentPanel,
                                final PersonFormModel personFormModel,final ModalMode mode) {

        super(name);

        boolean enableForm = true;

        if (mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(INSERT)) {
            // add an initial notification, effectively require at least one
            personFormModel.getNotificationConfigList().add(new ConfDataNotifConfig());
        }
        else if(mode.equals(UPDATE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            enableForm = personFormModel.initNotificationConfigList(confDataDAO);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<PersonFormModel> form = new Form<PersonFormModel>("inputForm", new CompoundPropertyModel<PersonFormModel>(personFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if(!enableForm) {
            feedback.error("Failed to load email notification list from the db, cannot edit record at this time"); 
            form.setEnabled(false);
        }

        final RequiredTextField<String> nickName = new RequiredTextField<String>("nickName");
        nickName.setOutputMarkupPlaceholderTag(true);
        form.add(nickName);

        final WebMarkupContainerWithAssociatedMarkup firstNameLabel = new WebMarkupContainerWithAssociatedMarkup("firstNameLabel");
        firstNameLabel.setOutputMarkupPlaceholderTag(true);
        form.add(firstNameLabel);

        final RequiredTextField<String> firstName = new RequiredTextField<String>("firstName");
        firstName.setOutputMarkupPlaceholderTag(true);
        form.add(firstName);

        final WebMarkupContainerWithAssociatedMarkup lastNameLabel = new WebMarkupContainerWithAssociatedMarkup("lastNameLabel");
        lastNameLabel.setOutputMarkupPlaceholderTag(true);
        form.add(lastNameLabel);

        final RequiredTextField<String> lastName = new RequiredTextField<String>("lastName");
        lastName.setOutputMarkupPlaceholderTag(true);
        form.add(lastName);

        final AjaxCheckBox isGroupAlias = new AjaxCheckBox("isGroupAlias") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

                Boolean checked = this.getModelObject();

                firstNameLabel.setVisible(!checked);
                firstName.setVisible(!checked);
                lastNameLabel.setVisible(!checked);
                lastName.setVisible(!checked);

                target.addComponent(firstNameLabel);
                target.addComponent(firstName);
                target.addComponent(lastNameLabel);
                target.addComponent(lastName);
            }
        };
        isGroupAlias.setOutputMarkupId(true);
        form.add(isGroupAlias);

        if (personFormModel.getIsGroupAlias()) {
            firstNameLabel.setVisible(false);
            firstName.setVisible(false);
            lastNameLabel.setVisible(false);
            lastName.setVisible(false);
        }


        final WebMarkupContainer notificationConfigListContainer = new WebMarkupContainer("notificationConfigListContainer");
        notificationConfigListContainer.setOutputMarkupId(true);
        form.add(notificationConfigListContainer);

        final ListView<ConfDataNotifConfig> notificationConfigList =
                new ListView<ConfDataNotifConfig>("notificationConfigList", personFormModel.getNotificationConfigList()) {

                    private Component hiddenRemoveButton = null;

                    @Override
                    public void populateItem(final ListItem item) {

                        final ListView<ConfDataNotifConfig> thisList = this;

                        final ConfDataNotifConfig config = (ConfDataNotifConfig) item.getModelObject();

                        // email text field
                        final RequiredTextField<String> address = new RequiredTextField<String>("address",new PropertyModel<String>(config,"address"));
                        address.setOutputMarkupId(true);
                        address.add(EmailAddressValidator.getInstance());
                        address.add(new AjaxFormValidatingBehavior(form,"onchange") {

                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                config.setAddress(address.getInput());
                            }
                        });
                        item.add(address);

                        // notification type drop down
                        final DropDownChoice<NotificationType> notificationTypeList = new DropDownChoice<NotificationType>("notifType",
                                new PropertyModel<NotificationType>(config,"notifType"),
                                Arrays.asList(NotificationType.values()));
                        notificationTypeList.setRequired(true);
                        notificationTypeList.setOutputMarkupId(true);
                        notificationTypeList.add(new AjaxFormComponentUpdatingBehavior("onchange") {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                Object obj = notificationTypeList.getModelObject();
                                NotificationType type = (NotificationType)obj;
                                config.setNotifType(type);
                            }
                        });
                        item.add(notificationTypeList);

                        // remove button
                        AjaxFallbackButton removeNotificationConfigButton =
                                new AjaxFallbackButton("removeNotificationConfig",form) {

                                    @Override
                                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                                        personFormModel.getNotificationConfigList().remove(item.getIndex());
                                        thisList.modelChanged();
                                        target.addComponent(notificationConfigListContainer);
                                    }
                                };

                        if (personFormModel.getNotificationConfigList().size() <= 1) {
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
                        personFormModel.getNotificationConfigList().add(new ConfDataNotifConfig());
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

                PersonFormModel person = (PersonFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;
                    if(mode.equals(INSERT)) {
                        success = person.insert(confDataDAO);
                    }
                    else if(mode.equals(UPDATE)){
                        success = person.update(confDataDAO);
                    }
                    else {
                        throw new IllegalStateException("Unrecognized mode: " + mode);
                    }

                    String statusMessage = person.getLastStatusMessage();

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
