package com.ning.arecibo.alertmanager.tabs.people;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;

import com.ning.arecibo.util.Logger;

import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.*;
import static com.ning.arecibo.alertmanager.utils.ModalWindowUtils.ModalMode.*;

public class PersonModalPage extends WebPage {
    final static Logger log = Logger.getLogger(PersonModalPage.class);

    public PersonModalPage(final ModalWindow window,final PeoplePanel parentPanel) {
        // insert by default
        this(window,parentPanel,new PersonFormModel(),INSERT);
    }

    public PersonModalPage(final ModalWindow window,final PeoplePanel parentPanel,PersonFormModel personFormModel,ModalMode mode) {

        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        if(mode.equals(INSERT) || mode.equals(UPDATE)) {
            PersonInputFormPanel panel = new PersonInputFormPanel("formPanel",feedback,window,parentPanel,personFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else if(mode.equals(DELETE)) {
            PersonDeleteFormPanel panel = new PersonDeleteFormPanel("formPanel",feedback,window,parentPanel,personFormModel,mode);
            panel.setOutputMarkupId(true);
            add(panel);
        }
        else {
            throw new IllegalStateException("Unrecognized mode: " + mode);
        }
    }
}
