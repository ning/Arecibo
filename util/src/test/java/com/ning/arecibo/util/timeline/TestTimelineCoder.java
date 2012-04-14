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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestTimelineCoder
{
    @Test(groups = "fast")
    public void testBasicEncodeDecode() throws Exception
    {
        final int firstTime = 1000000;
        final int[] unencodedTimes = new int[]{firstTime, firstTime + 30, firstTime + 60, firstTime + 90, firstTime + 1000, firstTime + 2000, firstTime + 2030, firstTime + 2060};

        final byte[] compressedTimes = TimelineCoder.compressTimes(unencodedTimes);
        //System.out.printf("Compressed times: %s\n", new String(Hex.encodeHex(compressedTimes)));
        final int[] decompressedTimes = TimelineCoder.decompressTimes(compressedTimes);
        Assert.assertEquals(decompressedTimes.length, unencodedTimes.length);
        for (int i = 0; i < unencodedTimes.length; i++) {
            Assert.assertEquals(decompressedTimes[i], unencodedTimes[i]);
        }
    }

    @Test(groups = "fast")
    public void testRepeats() throws Exception
    {
        final int firstTime = 1293846;
        final int[] unencodedTimes = new int[]{firstTime, firstTime + 5, firstTime + 5, firstTime + 5, firstTime + 1000, firstTime + 1000, firstTime + 2030, firstTime + 2060};

        final byte[] compressedTimes = TimelineCoder.compressTimes(unencodedTimes);
        final int[] decompressedTimes = TimelineCoder.decompressTimes(compressedTimes);
        Assert.assertEquals(decompressedTimes.length, unencodedTimes.length);
        for (int i = 0; i < unencodedTimes.length; i++) {
            Assert.assertEquals(decompressedTimes[i], unencodedTimes[i]);
        }
    }

    @Test(groups = "fast")
    public void testCombiningTimelinesByteRepeats() throws Exception
    {
        final int firstTime = 1293846;
        final int[] unencodedTimes1 = new int[10];
        final int[] unencodedTimes2 = new int[10];
        for (int i=0; i<10; i++) {
            unencodedTimes1[i] = firstTime + i * 100;
            unencodedTimes2[i] = firstTime + 10 * 100 + i * 100;
        }
        final byte[] compressedTimes1 = TimelineCoder.compressTimes(unencodedTimes1);
        final byte[] compressedTimes2 = TimelineCoder.compressTimes(unencodedTimes2);
        Assert.assertEquals(compressedTimes1.length, 8);
        Assert.assertEquals(compressedTimes1[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(compressedTimes1[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_BYTE.getOpcodeIndex());
        Assert.assertEquals(compressedTimes1[6] & 0xff, 9);
        Assert.assertEquals(compressedTimes1[7] & 0xff, 100);
        Assert.assertEquals(compressedTimes2.length, 8);
        Assert.assertEquals(compressedTimes2[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(compressedTimes2[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_BYTE.getOpcodeIndex());
        Assert.assertEquals(compressedTimes2[6] & 0xff, 9);
        Assert.assertEquals(compressedTimes2[7] & 0xff, 100);
        final List<byte[]> timesList = new ArrayList<byte[]>();
        timesList.add(compressedTimes1);
        timesList.add(compressedTimes2);
        final byte[] combinedTimes = TimelineCoder.combineTimelines(timesList);
        Assert.assertEquals(combinedTimes.length, 8);
        Assert.assertEquals(combinedTimes[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(combinedTimes[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_BYTE.getOpcodeIndex());
        Assert.assertEquals(combinedTimes[6] & 0xff, 19);
        Assert.assertEquals(combinedTimes[7] & 0xff, 100);
        // Check for 19, not 20, since the first full time took one
        Assert.assertEquals(combinedTimes[6], 19);
    }

    @Test(groups = "fast")
    public void testCombiningTimelinesShortRepeats() throws Exception
    {
        final int sampleCount = 240;
        final int firstTime = 1293846;
        final int[] unencodedTimes1 = new int[240];
        final int[] unencodedTimes2 = new int[240];
        for (int i=0; i<sampleCount; i++) {
            unencodedTimes1[i] = firstTime + i * 100;
            unencodedTimes2[i] = firstTime + sampleCount * 100 + i * 100;
        }
        final byte[] compressedTimes1 = TimelineCoder.compressTimes(unencodedTimes1);
        final byte[] compressedTimes2 = TimelineCoder.compressTimes(unencodedTimes2);
        Assert.assertEquals(compressedTimes1.length, 8);
        Assert.assertEquals(compressedTimes1[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(compressedTimes1[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_BYTE.getOpcodeIndex());
        Assert.assertEquals(compressedTimes1[6] & 0xff, sampleCount - 1);
        Assert.assertEquals(compressedTimes1[7] & 0xff, 100);
        Assert.assertEquals(compressedTimes2.length, 8);
        Assert.assertEquals(compressedTimes2[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(compressedTimes2[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_BYTE.getOpcodeIndex());
        Assert.assertEquals(compressedTimes2[6] & 0xff, sampleCount - 1);
        Assert.assertEquals(compressedTimes2[7] & 0xff, 100);
        final List<byte[]> timesList = new ArrayList<byte[]>();
        timesList.add(compressedTimes1);
        timesList.add(compressedTimes2);
        final byte[] combinedTimes = TimelineCoder.combineTimelines(timesList);
        Assert.assertEquals(combinedTimes.length, 9);
        Assert.assertEquals(combinedTimes[0] & 0xff, TimelineOpcode.FULL_TIME.getOpcodeIndex());
        Assert.assertEquals(combinedTimes[5] & 0xff, TimelineOpcode.REPEATED_DELTA_TIME_SHORT.getOpcodeIndex());
        Assert.assertEquals(combinedTimes[6] & 0xff, 1);
        Assert.assertEquals(combinedTimes[7] & 0xff, sampleCount * 2 - 1 - 256);
        Assert.assertEquals(combinedTimes[8], 100);
    }

    @Test(groups = "fast")
    public void testCombiningShortFragments() throws Exception
    {
        final byte[] fragment0 = new byte[] { (byte)-1, (byte)0, (byte)15, (byte)66, (byte)84, (byte)20 };
        final byte[] fragment1 = new byte[] { (byte)-1, (byte)0, (byte)15, (byte)66, (byte)-122, (byte)30 };
        final byte[] fragment2 = new byte[] { (byte)-1, (byte)0, (byte)15, (byte)66, (byte)-62, (byte)30 };
        final byte[] fragment3 = new byte[] { (byte)-1, (byte)0, (byte)15, (byte)66, (byte)-2, (byte)30 };
        final byte[] [] fragmentArray = new byte[] [] { fragment0, fragment1, fragment2, fragment3};
        final byte[] combined = TimelineCoder.combineTimelines(Arrays.asList(fragmentArray));
        final int[] intTimes = TimelineCoder.decompressTimes(combined);
        final List<int[]> fragmentIntTimes = new ArrayList<int[]>();
        final List<Integer> allFragmentTimes = new ArrayList<Integer>();
        int totalLength = 0;
        for (int i=0; i<fragmentArray.length; i++) {
            final int[] fragmentTimes = TimelineCoder.decompressTimes(fragmentArray[i]);
            fragmentIntTimes.add(fragmentTimes);
            totalLength += fragmentTimes.length;
            for (int time : fragmentTimes) {
                allFragmentTimes.add(time);
            }
        }
        Assert.assertEquals(intTimes.length, totalLength);
        for (int i=0; i<totalLength; i++) {
            Assert.assertEquals(intTimes[i], (int)allFragmentTimes.get(i));
        }
    }

    @Test(groups = "fast")
    public void testCombiningTimelinesRandomRepeats() throws Exception
    {
        final int[] increments = new int[] { 30, 45, 10, 30, 20 };
        final int[] repetitions = new int[] { 1, 2, 3, 4, 5, 240, 250, 300 };
        final int firstTimeInt = 1000000;
        final DateTime startTime = DateTimeUtils.dateTimeFromUnixSeconds(firstTimeInt);
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final Random rand = new Random(0);
        DateTime nextTime = startTime;
        int count = 0;
        for (int i=0; i<20; i++) {
            final int increment = increments[rand.nextInt(increments.length)];
            final int repetition = repetitions[rand.nextInt(repetitions.length)];
            for (int r=0; i<repetition; i++) {
                nextTime = nextTime.plusSeconds(increment);
                dateTimes.add(nextTime);
                count++;
            }
        }
        final byte[] allCompressedTime = TimelineCoder.compressDateTimes(dateTimes);
        final int[] allIntTimes = TimelineCoder.decompressTimes(allCompressedTime);
        Assert.assertEquals(allIntTimes.length, dateTimes.size());
        for (int i=0; i<count; i++) {
            Assert.assertEquals(allIntTimes[i], DateTimeUtils.unixSeconds(dateTimes.get(i)));
        }
        for (int fragmentLength=2; fragmentLength<count/2; fragmentLength++) {
            final List<byte[]> fragments = new ArrayList<byte[]>();
            final int fragmentCount = (int)Math.ceil((double)count / (double)fragmentLength);
            for (int fragCounter=0; fragCounter<fragmentCount; fragCounter++) {
                final int fragIndex = fragCounter * fragmentLength;
                final List<DateTime> fragment = dateTimes.subList(fragIndex, Math.min(count, fragIndex + fragmentLength));
                fragments.add(TimelineCoder.compressDateTimes(fragment));
            }
            final byte[] combined = TimelineCoder.combineTimelines(fragments);
            final int[] intTimes = TimelineCoder.decompressTimes(combined);
            //Assert.assertEquals(intTimes.length, count);
            for (int i=0; i<count; i++) {
                Assert.assertEquals(intTimes[i], DateTimeUtils.unixSeconds(dateTimes.get(i)));
            }
        }
    }

    @Test(groups = "fast")
    public void test65KRepeats() throws Exception
    {
        int count = 0;
        final List<DateTime> dateTimes = new ArrayList<DateTime>();
        final DateTime startingDateTime = DateTimeUtils.dateTimeFromUnixSeconds(1000000);
        DateTime time = startingDateTime;
        for (int i=0; i<20; i++) {
            time = time.plusSeconds(200);
            dateTimes.add(time);
        }
        for (int i=0; i<0xFFFF + 100; i++) {
            time = time.plusSeconds(100);
            dateTimes.add(time);
        }
        final byte[] timeBytes = TimelineCoder.compressDateTimes(dateTimes);
        final String hex = new String(Hex.encodeHex(timeBytes));
        // Here are the compressed samples: ff000f4308fe13c8fdffff64fe6464
        // Translation:
        // [ff 00 0f 43 08] means absolution time 1000000
        // [fe 13 c8] means repeat 19 times delta 200 seconds
        // [fd ff ff 64] means repeat 65525 times delta 100 seconds
        // [fe 64 64] means repeat 100 times delta 100 seconds
        Assert.assertEquals(timeBytes, Hex.decodeHex("ff000f4308fe13c8fdffff64fe6464".toCharArray()));
        final int[] restoredSamples = TimelineCoder.decompressTimes(timeBytes);
        Assert.assertEquals(restoredSamples.length, dateTimes.size());
        for (int i=0; i<count; i++) {
            Assert.assertEquals(restoredSamples[i], DateTimeUtils.unixSeconds(dateTimes.get(i)));
        }
    }
}
