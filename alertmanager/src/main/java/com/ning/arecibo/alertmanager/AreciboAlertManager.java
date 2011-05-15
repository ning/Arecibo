package com.ning.arecibo.alertmanager;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.util.time.Duration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.alertmanager.guice.AlertManagerModule;
import com.ning.arecibo.alertmanager.tabs.TabbedPanelPage;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.lifecycle.LifecycleModule;

public class AreciboAlertManager extends WebApplication
{
    final static Logger log = Logger.getLogger(AreciboAlertManager.class);

    private volatile ConfDataDAO confDataDAO;
    private volatile AreciboAlertManagerConfig config;

    @Override
    public void init() {

        String resourceType = getConfigurationType();
        if(resourceType.equalsIgnoreCase("DEVELOPMENT")) {
            log.info("Setting development mode resource folder: 'src/main/java'");
            getResourceSettings().setResourcePollFrequency(Duration.milliseconds(500));
            getResourceSettings().addResourceFolder("src/main/java");
        }

        getApplicationSettings().setPageExpiredErrorPage(getHomePage());

        Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new LifecycleModule(),
            new AlertDataModule(),
            new AlertManagerModule());

        confDataDAO = injector.getInstance(ConfDataDAO.class);
        config = injector.getInstance(AreciboAlertManagerConfig.class);
    }

    @Override
    public Session newSession(Request request, Response response) {
        return new AreciboAlertManagerSession(request);
    }
    
    @Override
	public Class<TabbedPanelPage> getHomePage()
	{
		return TabbedPanelPage.class;
	}

    public ConfDataDAO getConfDataDAO() {
        return this.confDataDAO;
    }

    public AreciboAlertManagerConfig getConfig() {
        return this.config;
    }
}
