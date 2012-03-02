package com.ning.arecibo.dashboard.context;

import com.google.inject.Injector;
import com.ning.arecibo.alert.confdata.guice.AlertDataModule;
import com.ning.arecibo.collector.CollectorClientConfig;
import com.ning.arecibo.dashboard.alert.AlertStatusManager;
import com.ning.arecibo.dashboard.format.DashboardFormatManager;
import com.ning.arecibo.dashboard.galaxy.GalaxyStatusManager;
import com.ning.arecibo.dashboard.guice.DashboardModule;
import com.ning.arecibo.event.publisher.EventPublisherConfig;
import com.ning.arecibo.event.publisher.HdfsEventPublisherModule;
import com.ning.arecibo.util.galaxy.GalaxyModule;
import com.ning.arecibo.util.lifecycle.Lifecycle;
import com.ning.arecibo.util.lifecycle.LifecycleEvent;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.jetty.base.modules.ServerModuleBuilder;
import com.ning.jetty.core.listeners.SetupServer;

import javax.servlet.ServletContextEvent;

public class DashboardStartupContextListener extends SetupServer
{
    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        final ServerModuleBuilder builder = new ServerModuleBuilder()
            .addConfig(CollectorClientConfig.class)
            .setAreciboProfile(System.getProperty("action.arecibo.profile", "ning.jmx:name=MonitoringProfile"))
            .addModule(new LifecycleModule())
            .addModule(new GalaxyModule())
            .addModule(new DashboardModule())
            .addModule(new HdfsEventPublisherModule("server", "dashboard"))
            .addModule(new AlertDataModule("arecibo.dashboard.alert.conf.db"))
            .enableLog4J()
            .addResource("com.ning.arecibo.dashboard.resources");

        guiceModule = builder.build();

        // Let Guice create the injector
        super.contextInitialized(event);

        final Injector injector = (Injector) event.getServletContext().getAttribute(Injector.class.getName());

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
}
