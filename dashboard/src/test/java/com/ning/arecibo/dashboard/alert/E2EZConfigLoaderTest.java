package com.ning.arecibo.dashboard.alert;

import org.testng.annotations.Test;
import com.ning.arecibo.dashboard.alert.e2ez.E2EZConfigManager;

import com.ning.arecibo.util.Logger;

public class E2EZConfigLoaderTest {
    private static final Logger log = Logger.getLogger(E2EZConfigLoaderTest.class);

    @Test(groups = "fast")
    public void testLoadBundledE2EZConfigs() {

        try {

            E2EZConfigManager manager = new E2EZConfigManager();
        }
        catch(RuntimeException ex) {
            // make it log the exception in the maven output
            log.warn(ex);
            throw(ex);
        }

    }
}
