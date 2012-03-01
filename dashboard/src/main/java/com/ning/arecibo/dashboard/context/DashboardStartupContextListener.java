package com.ning.arecibo.dashboard.context;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.guice.DashboardModule;
import com.ning.arecibo.util.galaxy.GalaxyModule;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;

import javax.management.MBeanServer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.management.ManagementFactory;

public class DashboardStartupContextListener implements ServletContextListener
{
    public void contextInitialized(final ServletContextEvent sce)
    {
        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new LifecycleModule(),
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(MBeanServer.class).toInstance(ManagementFactory.getPlatformMBeanServer());
                }
            },
            new GalaxyModule(),
            new DashboardModule(),
            new AlertDataModule("arecibo.dashboard.alert.conf.db")
        );

        final GalaxyStatusManager galaxyStatusManager = injector.getInstance(GalaxyStatusManager.class);
        galaxyStatusManager.start();

        final AlertStatusManager alertStatusManager = injector.getInstance(AlertStatusManager.class);
        alertStatusManager.start();

        final DashboardFormatManager dashboardFormatManager = injector.getInstance(DashboardFormatManager.class);
        dashboardFormatManager.init();

        // do this here for now, should really be part of better guiciness for servlet context
        // lifecycle is needed, if for no other reason than to start the LoggingModule
        final Lifecycle lc = injector.getInstance(Lifecycle.class);
        lc.fire(LifecycleEvent.START);
    }

    public void contextDestroyed(ServletContextEvent sce)
    {
    }
}
