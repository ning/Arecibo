package com.ning.arecibo.alertmanager.guice;

import org.skife.config.ConfigurationObjectFactory;
import com.google.inject.AbstractModule;
import com.ning.arecibo.alertmanager.AreciboAlertManagerConfig;
import com.ning.arecibo.util.Logger;

public class AlertManagerModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AlertManagerModule.class);

    @Override
	public void configure()
	{
        AreciboAlertManagerConfig config = new ConfigurationObjectFactory(System.getProperties()).build(AreciboAlertManagerConfig.class);

        bind(AreciboAlertManagerConfig.class).toInstance(config);
	}
}
