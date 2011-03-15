package com.ning.arecibo.alertmanager.tabs.managingkey;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ManagingKeyModalPage extends WebPage {
    final static Logger log = Logger.getLogger(ManagingKeyModalPage.class);

    public ManagingKeyModalPage(final ModalWindow window,final ManagingKeyPanel parentPanel) {
        // insert by default
        this(window,parentPanel,new ManagingKeyFormModel(),INSERT);
    }

    public ManagingKeyModalPage(final ModalWindow window,final ManagingKeyPanel parentPanel,ManagingKeyFormModel managingKeyFormModel,ModalMode mode) {

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        if(mode.equals(INSERT) || mode.equals(UPDATE)) {
            ManagingKeyInputFormPanel panel = new ManagingKeyInputFormPanel("formPanel",feedback,window,parentPanel,managingKeyFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else if(mode.equals(DELETE)) {
            ManagingKeyDeleteFormPanel panel = new ManagingKeyDeleteFormPanel("formPanel",feedback,window,parentPanel,managingKeyFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }
    }
}
