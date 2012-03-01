package com.ning.arecibo.dashboard.context;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.guice.DashboardModule;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.galaxy.GalaxyModule;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;

public class DashboardStartupContextListener implements ServletContextListener
{
	private final static Logger log = Logger.getLogger(DashboardStartupContextListener.class);
	
	public void contextInitialized(ServletContextEvent sce) {
	    
        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                        new LifecycleModule(),
                        new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                            }
                        },
                        new GalaxyModule(),
                        // TODO: need to bind an implementation of ServiceLocator
                        new DashboardModule(),
                        new AlertDataModule("arecibo.dashboard.alert.conf.db")
                );

		ContextMbeanManager mbeanManager = injector.getInstance(ContextMbeanManager.class);

        GalaxyStatusManager galaxyStatusManager = injector.getInstance(GalaxyStatusManager.class);
        galaxyStatusManager.start();

		AlertStatusManager alertStatusManager = injector.getInstance(AlertStatusManager.class);
		alertStatusManager.start();

		DashboardFormatManager dashboardFormatManager = injector.getInstance(DashboardFormatManager.class);
		dashboardFormatManager.init();

		// do this here for now, should really be part of better guiciness for servlet context
		// lifecycle is needed, if for no other reason than to start the LoggingModule
		Lifecycle lc = injector.getInstance(Lifecycle.class);
		lc.fire(LifecycleEvent.START);
	}

	public void contextDestroyed(ServletContextEvent sce) {
    }
}
