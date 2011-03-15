package com.ning.arecibo.alertmanager;

import com.google.inject.Inject;
import com.ning.arecibo.alertmanager.guice.GeneralTableDisplayRows;
import com.ning.arecibo.alertmanager.guice.ThresholdsTableDisplayRows;

public class AreciboAlertManagerConfigProps {

    private final int generalTableDisplayRows;
    private final int thresholdsTableDisplayRows;

    @Inject
    public AreciboAlertManagerConfigProps(@GeneralTableDisplayRows int generalTableDisplayRows,
                                          @ThresholdsTableDisplayRows int thresholdsTableDisplayRows) {

        this.generalTableDisplayRows = generalTableDisplayRows;
        this.thresholdsTableDisplayRows = thresholdsTableDisplayRows;
    }

    public int getGeneralTableDisplayRows() {
        return generalTableDisplayRows;
    }

    public int getThresholdsTableDisplayRows() {
        return thresholdsTableDisplayRows;
    }
}
