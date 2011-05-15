package com.ning.arecibo.alertmanager;

import org.skife.config.Config;
import org.skife.config.Default;

public interface AreciboAlertManagerConfig {
    @Config("arecibo.alertmanager.general_table_display_rows")
    @Default("25")
    int getGeneralTableDisplayRows();

    @Config("arecibo.alertmanager.thresholds_table_display_rows")
    @Default("10")
    int getThresholdsTableDisplayRows();
}
