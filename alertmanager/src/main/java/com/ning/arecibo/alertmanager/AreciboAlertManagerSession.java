package com.ning.arecibo.alertmanager;

import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.Request;
import com.ning.arecibo.alertmanager.tabs.NavigableTabbedPanel;

public class AreciboAlertManagerSession extends WebSession {

    protected AreciboAlertManagerSession(Request request) {
        super(request);
    }

    public NavigableTabbedPanel getNavigableTabbedPanel() {
        return (NavigableTabbedPanel)super.getAttribute("navigableTabbedPanel");
    }

    public void setNavigableTabbedPanel(NavigableTabbedPanel navigableTabbedPanel) {
        super.setAttribute("navigableTabbedPanel",navigableTabbedPanel);
    }
}
