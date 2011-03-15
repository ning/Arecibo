package com.ning.arecibo.alertmanager.tabs.notificationgroups;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class NotificationGroupModalPage extends WebPage {
    final static Logger log = Logger.getLogger(NotificationGroupModalPage.class);

    public NotificationGroupModalPage(final ModalWindow window,final NotificationGroupsPanel parentPanel) {
        // insert by default
        this(window,parentPanel,new NotificationGroupFormModel(),INSERT);
    }

    public NotificationGroupModalPage(final ModalWindow window,final NotificationGroupsPanel parentPanel,NotificationGroupFormModel notificationGroupFormModel,ModalMode mode) {

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        if(mode.equals(INSERT) || mode.equals(UPDATE)) {
            NotificationGroupInputFormPanel panel = new NotificationGroupInputFormPanel("formPanel",feedback,window,parentPanel,notificationGroupFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else if(mode.equals(DELETE)) {
            NotificationGroupDeleteFormPanel panel = new NotificationGroupDeleteFormPanel("formPanel",feedback,window,parentPanel,notificationGroupFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }
    }
}
