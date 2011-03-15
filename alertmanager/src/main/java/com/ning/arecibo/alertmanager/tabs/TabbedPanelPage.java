package com.ning.arecibo.alertmanager.tabs;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageParameters;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.Model;
import com.ning.arecibo.alertmanager.AreciboAlertManagerSession;
import com.ning.arecibo.alertmanager.tabs.activealerts.ActiveAlertsPanel;
import com.ning.arecibo.alertmanager.tabs.alertincidents.AlertIncidentLogsPanel;
import com.ning.arecibo.alertmanager.tabs.alertingconfigs.AlertingConfigsPanel;
import com.ning.arecibo.alertmanager.tabs.home.HomePanel;
import com.ning.arecibo.alertmanager.tabs.managingkey.ManagingKeyPanel;
import com.ning.arecibo.alertmanager.tabs.notificationgroups.NotificationGroupsPanel;
import com.ning.arecibo.alertmanager.tabs.people.PeoplePanel;
import com.ning.arecibo.alertmanager.tabs.thresholds.ThresholdsPanel;
import com.ning.arecibo.util.Logger;


// this all copied initially from the tabs-examples tabpanel example
public class TabbedPanelPage extends WebPage
{
    final static Logger log = Logger.getLogger(TabbedPanelPage.class); 

	public TabbedPanelPage()
	{
        // log the page params
        if(log.isDebugEnabled()) {
            log.debug("Page params:");
            PageParameters params = getPageParameters();
            if(params != null) {
                for(String key:params.keySet()) {
                    Object value = params.get(key);
                    log.debug("\t" + key + " = " + value.toString());
                }
            }
            else {
                log.debug("\tNo page params");
            }
        }

        AreciboAlertManagerSession session = (AreciboAlertManagerSession)this.getSession();

        NavigableTabbedPanel tabbedPanel = session.getNavigableTabbedPanel();
        if(tabbedPanel == null) {
            tabbedPanel = getNewTabbedPanel();
            session.setNavigableTabbedPanel(tabbedPanel);
        }

        add(tabbedPanel);
	}

    private NavigableTabbedPanel getNewTabbedPanel() {

        // create a list of ITab objects used to feed the tabbed panel
        List<ITab> tabs = new ArrayList<ITab>();

        tabs.add(new AbstractTab(new Model<String>("Home"))
        {
            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new HomePanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });

        /*
        tabs.add(new AbstractTab(new Model<String>("Active Alerts"))
        {
            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new ActiveAlertsPanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });
        */

        tabs.add(new AbstractTab(new Model<String>("Alert Activity")) {
            @Override
            public Panel getPanel(String panelId) {

                // don't lazy load this one, since it has a sub-lazy loading anyway
                return new AlertIncidentLogsPanel(panelId);
            }
        });

        tabs.add(new AbstractTab(new Model<String>("Alerting Configurations"))
        {
            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new AlertingConfigsPanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });

        tabs.add(new AbstractTab(new Model<String>("Threshold Definitions"))
        {
            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new ThresholdsPanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });

        tabs.add(new AbstractTab(new Model<String>("Suppress Alerts"))
        {
            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new ManagingKeyPanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });

        tabs.add(new AbstractTab(new Model<String>("Notification Groups"))
        {

            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new NotificationGroupsPanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }

        });

        tabs.add(new AbstractTab(new Model<String>("People & Aliases"))
        {

            @Override
            public Panel getPanel(String panelId)
            {
                return new AjaxLazyLoadPanel(panelId) {

                    @Override
                    public Component getLazyLoadComponent(String panelId) {
                        return new PeoplePanel(panelId);
                    }

                    @Override
                    public Component getLoadingComponent(String panelId) {
                        return new LazyLoadingWaitPanel(panelId);
                    }
                };
            }
        });



        // set the tabpanel style based on one of the choices in the css (e.g. tabpanel1)
        NavigableTabbedPanel tabbedPanel = new NavigableTabbedPanel("tabs", tabs);

        tabbedPanel.add(new AttributeModifier("class", true,new Model<String>("tabpanel1")));

        return tabbedPanel;
    }

}
