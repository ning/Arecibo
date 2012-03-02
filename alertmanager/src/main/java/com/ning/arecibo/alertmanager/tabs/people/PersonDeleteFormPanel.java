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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class PersonDeleteFormPanel extends Panel {
    final static Logger log = Logger.getLogger(PersonDeleteFormPanel.class);

    public PersonDeleteFormPanel(String name,final FeedbackPanel feedback,final ModalWindow window,final PeoplePanel parentPanel,
                                                final PersonFormModel personFormModel,final ModalMode mode) {

        super(name);

        boolean enableForm = true;

        if (mode == null) {
            throw new IllegalStateException();
        }
        else if (mode.equals(DELETE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            enableForm = personFormModel.initNotificationConfigList(confDataDAO);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<PersonFormModel> form = new Form<PersonFormModel>("deleteForm",new CompoundPropertyModel<PersonFormModel>(personFormModel));
        this.setOutputMarkupId(true);
        add(form);

        if(!enableForm) {
            feedback.error("Failed to load email notification list from the db, cannot edit record at this time");
            form.setEnabled(false);
        }

        Label nickName = new Label("nickName");
        form.add(nickName);

        final WebMarkupContainerWithAssociatedMarkup firstNameLabel = new WebMarkupContainerWithAssociatedMarkup("firstNameLabel");
        form.add(firstNameLabel);

        final Label firstName = new Label("firstName");
        form.add(firstName);

        final WebMarkupContainerWithAssociatedMarkup lastNameLabel = new WebMarkupContainerWithAssociatedMarkup("lastNameLabel");
        lastNameLabel.setOutputMarkupPlaceholderTag(true);
        form.add(lastNameLabel);

        final Label lastName = new Label("lastName");
        form.add(lastName);

        final Label isGroupAlias = new Label("isGroupAlias");
        form.add(isGroupAlias);

        if(personFormModel.getIsGroupAlias()) {
            firstNameLabel.setVisible(false);
            firstName.setEnabled(false);
            lastNameLabel.setVisible(false);
            lastName.setEnabled(false);
        }


        final ListView<ConfDataNotifConfig> notificationConfigList = new ListView<ConfDataNotifConfig>("notificationConfigList", personFormModel.getNotificationConfigList()) {

            @Override
            public void populateItem(final ListItem item) {

                final ListView<ConfDataNotifConfig> thisList = this;

                final ConfDataNotifConfig config = (ConfDataNotifConfig) item.getModelObject();

                final WebMarkupContainerWithAssociatedMarkup addressLabel = new WebMarkupContainerWithAssociatedMarkup("addressLabel");
                item.add(addressLabel);

                final Label address = new Label("address", new PropertyModel(config, "address"));
                item.add(address);
            }
        };
        form.add(notificationConfigList);


        final Label warningLabel = new Label("submissionWarningMessage", "There was an issue with deletinsavingg this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("deleteButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                PersonFormModel person = (PersonFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
                    boolean success;

                    if(mode.equals(DELETE)) {
                        success = person.delete(confDataDAO);
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
