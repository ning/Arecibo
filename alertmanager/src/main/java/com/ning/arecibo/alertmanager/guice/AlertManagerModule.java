package com.ning.arecibo.alertmanager.guice;

import com.google.inject.AbstractModule;
import com.ning.arecibo.alertmanager.AreciboAlertManagerConfigProps;
import com.ning.arecibo.util.Logger;

public class AlertManagerModule extends AbstractModule
{
    final static Logger log = Logger.getLogger(AlertManagerModule.class);

    @Override
	public void configure()
	{
        bindConstant().annotatedWith(GeneralTableDisplayRows.class).to(Integer.getInteger("arecibo.alertmanager.general_table_display_rows", GeneralTableDisplayRows.DEFAULT));
        bindConstant().annotatedWith(ThresholdsTableDisplayRows.class).to(Integer.getInteger("arecibo.alertmanager.thresholds_table_display_rows", ThresholdsTableDisplayRows.DEFAULT));

        bind(AreciboAlertManagerConfigProps.class).asEagerSingleton();
	}
}
