package com.ning.arecibo.alertmanager.tabs;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.extensions.ajax.markup.html.AjaxIndicatorAppender;
import org.apache.wicket.ajax.IAjaxIndicatorAware;

public class LazyLoadingWaitPanel extends Panel {

    public LazyLoadingWaitPanel(String panelId) {
        super(panelId);
    }
}

