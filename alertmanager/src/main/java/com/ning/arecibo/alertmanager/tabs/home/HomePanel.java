package com.ning.arecibo.alertmanager.tabs.home;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.MarkupContainer;
import com.ning.arecibo.alertmanager.AreciboAlertManagerSession;
import com.ning.arecibo.alertmanager.tabs.NavigableTabbedPanel;

import com.ning.arecibo.util.Logger;


public class HomePanel extends Panel{
    final static Logger log = Logger.getLogger(HomePanel.class);

    public HomePanel(String id) {
        super(id);

        AreciboAlertManagerSession session = (AreciboAlertManagerSession)this.getSession();
        final NavigableTabbedPanel parent = session.getNavigableTabbedPanel();

        int tabIndex = 1;

        //add(parent.newLink("activeAlertsLink",tabIndex++));
        add(parent.newLink("logsLink",tabIndex++));
        add(parent.newLink("alertingConfigLink",tabIndex++));
        add(parent.newLink("thresholdLink",tabIndex++));
        add(parent.newLink("managingKeyLink",tabIndex++));
        add(parent.newLink("notificationGroupLink",tabIndex++));
        add(parent.newLink("peopleLink",tabIndex++));
    }
}
