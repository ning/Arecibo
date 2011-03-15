package com.ning.arecibo.alertmanager.tabs;

import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;

public class NavigableTabbedPanel extends AjaxTabbedPanel {

    public NavigableTabbedPanel(String id, List<ITab> tabs) {
        super(id,tabs);
    }

    @Override
    public WebMarkupContainer newLink(String linkId,final int index) {
        return super.newLink(linkId,index);
    }
}
