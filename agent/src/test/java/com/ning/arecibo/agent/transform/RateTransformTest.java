/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

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
