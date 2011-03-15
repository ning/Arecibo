package com.ning.arecibo.alertmanager.tabs.thresholds;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class ThresholdModalPage extends WebPage {
    final static Logger log = Logger.getLogger(ThresholdModalPage.class);

    public ThresholdModalPage(final ModalWindow window,final ThresholdsPanel parentPanel) {
        // insert by default
        this(window,parentPanel,new ThresholdFormModel(),INSERT);
    }

    public ThresholdModalPage(final ModalWindow window,final ThresholdsPanel parentPanel,ThresholdFormModel thresholdFormModel,ModalMode mode) {

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        if(mode.equals(INSERT) || mode.equals(UPDATE)) {
            ThresholdInputFormPanel panel = new ThresholdInputFormPanel("formPanel",feedback,window,parentPanel,thresholdFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else if(mode.equals(DELETE)) {
            ThresholdDeleteFormPanel panel = new ThresholdDeleteFormPanel("formPanel",feedback,window,parentPanel,thresholdFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }
    }
}
