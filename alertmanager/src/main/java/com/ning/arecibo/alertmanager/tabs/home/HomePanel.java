/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
