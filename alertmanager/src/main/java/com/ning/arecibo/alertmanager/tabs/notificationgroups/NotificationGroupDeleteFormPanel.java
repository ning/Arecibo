package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.objects.ConfDataNotifConfig;
import com.ning.arecibo.alertmanager.AreciboAlertManager;

import com.ning.arecibo.util.Logger;


import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class NotificationGroupDeleteFormPanel extends Panel {
    final static Logger log = Logger.getLogger(NotificationGroupDeleteFormPanel.class);

    public NotificationGroupDeleteFormPanel(String name,final FeedbackPanel feedback,final ModalWindow window,final NotificationGroupsPanel parentPanel,
                                            NotificationGroupFormModel notifGroupFormModel,final ModalMode mode) {

        super(name);

        boolean enableForm = true;

        if(mode == null) {
            throw new IllegalStateException();
        }
        else if(mode.equals(DELETE)) {
            // initialize with current notification config list from db
            ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();
            enableForm = notifGroupFormModel.initNotificationConfigList(confDataDAO);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }

        final Form<NotificationGroupFormModel> form = new Form<NotificationGroupFormModel>("deleteForm",new CompoundPropertyModel<NotificationGroupFormModel>(notifGroupFormModel));
        form.setOutputMarkupId(true);
        add(form);

        if (!enableForm) {
            feedback.error("Failed to load email notification list from the db, cannot create alert group at this time");
            form.setEnabled(false);
        }

        final Label groupName = new Label("groupName");
        form.add(groupName);

        final Label enabled = new Label("enabled");
        form.add(enabled);

        final ListView<ConfDataNotifConfig> notificationConfigList =
                new ListView<ConfDataNotifConfig>("notificationConfigList", notifGroupFormModel.getNotificationConfigList()) {

                    @Override
                    public void populateItem(final ListItem item) {

                        final ConfDataNotifConfig config = (ConfDataNotifConfig) item.getModelObject();

                        final Label address = new Label("address",new PropertyModel(config,"address"));
                        item.add(address);
                    }
                };
        notificationConfigList.setReuseItems(true);
        notificationConfigList.setOutputMarkupId(true);
        form.add(notificationConfigList);

        final Label warningLabel = new Label("submissionWarningMessage","There was an issue with saving this data, see top of form for details");
        warningLabel.setOutputMarkupPlaceholderTag(true);
        warningLabel.setVisible(false);
        form.add(warningLabel);

        form.add(new AjaxFallbackButton("deleteButton", form) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {

                NotificationGroupFormModel notifGroup = (NotificationGroupFormModel)form.getModel().getObject();

                try {
                    ConfDataDAO confDataDAO = ((AreciboAlertManager) getApplication()).getConfDataDAO();

                    boolean success;
                    if(mode.equals(DELETE)) {
                        success = notifGroup.delete(confDataDAO);
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
