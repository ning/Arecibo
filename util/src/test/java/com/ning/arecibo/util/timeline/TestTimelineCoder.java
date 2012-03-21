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

package com.ning.arecibo.util.timeline;

import org.apache.commons.codec.binary.Hex;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTimelineCoder {
    private static int firstTime = 1000000;
    private static int[] unencodedTimes = new int[] { firstTime, firstTime + 30, firstTime + 60, firstTime + 90, firstTime + 1000, firstTime + 2000, firstTime + 2030, firstTime + 2060 };

    @Test(groups="fast")
    public void testBasicEncodeDecode() throws Exception {
        final byte[] compressedTimes = TimelineCoder.compressTimes(unencodedTimes);
        System.out.printf("Compressed times: %s\n", new String(Hex.encodeHex(compressedTimes)));
        final int[] decompressedTimes = TimelineCoder.decompressTimes(compressedTimes);
        Assert.assertEquals(decompressedTimes.length, unencodedTimes.length);
        for (int i=0; i<unencodedTimes.length; i++) {
            Assert.assertEquals(decompressedTimes[i], unencodedTimes[i]);
        }
    }
}
