package com.ning.arecibo.agent.transform;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;
import com.ning.arecibo.agent.transform.RateTransform;

/**
 * RateTransformTest
 * <p/>
 * <p/>
 * <p/>
 * Author: gary
 * Date: Sep 17, 2008
 * Time: 5:34:26 PM
 */
public class RateTransformTest
{
    @Test(groups = "fast")
    public void nullStartTest()
    {
        RateTransform rt = new RateTransform();
        assertEquals(null, rt.process(new Double(12)));
    }

    @Test(groups = "slow")
    public void objectTypeTest()
    {
        RateTransform rt = new RateTransform();
        assertEquals(null, rt.process(new Double(12)));
        sleep(10);
        assertEquals(Double.class, rt.process(new Double(12)).getClass());
    }

    @Test(groups = "slow")
    public void integerTypeTest()
    {
        RateTransform rt = new RateTransform();
        assertEquals(null, rt.process(new Integer(12)));
        sleep(10);
        assertEquals(Double.class, rt.process(new Integer(2)).getClass());
    }


    @Test(groups = "slow", expectedExceptions = IllegalArgumentException.class)
    public void nullTypeTest()
    {
        RateTransform rt = new RateTransform();
        assertEquals(null, rt.process(new Double(12)));
        sleep(10);
        rt.process(new Double("foo"));
    }



    @Test(groups = "slow")
    public void rateTest1()
    {
        RateTransform rt = new RateTransform();
        assertEquals(null, rt.process(new Double(12)));
        sleep(10);
        assertEquals(0.0, rt.process(new Double(12)));      // no change; expect 0.0, so checks for rounding errors
        sleep(10);
        assertTrue((Double) rt.process(new Double(0.0)) < 0.0);
        sleep(10);
        assertTrue((Double) rt.process(new Double(10.0)) > 0.0);
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
