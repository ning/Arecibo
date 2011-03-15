package com.ning.arecibo.alertmanager.tabs.alertingconfigs;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class AlertingConfigModalPage extends WebPage {
    final static Logger log = Logger.getLogger(AlertingConfigModalPage.class);

    public AlertingConfigModalPage(final ModalWindow window,final AlertingConfigsPanel parentPanel,AlertingConfigFormModel alertingConfigFormModel,ModalMode mode) {

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        if(mode.equals(INSERT) || mode.equals(UPDATE)) {
            AlertingConfigInputFormPanel panel = new AlertingConfigInputFormPanel("formPanel",feedback,window,parentPanel,alertingConfigFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else if(mode.equals(DELETE)) {
            AlertingConfigDeleteFormPanel panel = new AlertingConfigDeleteFormPanel("formPanel",feedback,window,parentPanel,alertingConfigFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }
    }
}
