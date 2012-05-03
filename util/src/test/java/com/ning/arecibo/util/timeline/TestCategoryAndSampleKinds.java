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

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCategoryAndSampleKinds
{
    @Test(groups = "fast")
    public void testMapping() throws Exception
    {
        final CategoryAndSampleKinds kinds = new CategoryAndSampleKinds("JVM");
        kinds.addSampleKind("GC");
        kinds.addSampleKind("CPU");

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(kinds);
        Assert.assertEquals("{\"eventCategory\":\"JVM\",\"sampleKinds\":[\"GC\",\"CPU\"]}", json);

        final CategoryAndSampleKinds kindsFromJson = mapper.readValue(json, CategoryAndSampleKinds.class);
        Assert.assertEquals(kindsFromJson, kinds);
    }

    @Test(groups = "fast")
    public void testComparison() throws Exception
    {
        final CategoryAndSampleKinds aKinds = new CategoryAndSampleKinds("JVM");
        aKinds.addSampleKind("GC");
        aKinds.addSampleKind("CPU");
        Assert.assertEquals(aKinds.compareTo(aKinds), 0);

        final CategoryAndSampleKinds bKinds = new CategoryAndSampleKinds("JVM");
        bKinds.addSampleKind("GC");
        bKinds.addSampleKind("CPU");
        Assert.assertEquals(aKinds.compareTo(bKinds), 0);
        Assert.assertEquals(bKinds.compareTo(aKinds), 0);

        final CategoryAndSampleKinds cKinds = new CategoryAndSampleKinds("JVM");
        cKinds.addSampleKind("GC");
        cKinds.addSampleKind("CPU");
        cKinds.addSampleKind("Something else");
        Assert.assertTrue(aKinds.compareTo(cKinds) < 0);
        Assert.assertTrue(cKinds.compareTo(aKinds) > 0);

        final CategoryAndSampleKinds dKinds = new CategoryAndSampleKinds("ZVM");
        dKinds.addSampleKind("GC");
        Assert.assertTrue(aKinds.compareTo(dKinds) < 0);
        Assert.assertTrue(dKinds.compareTo(aKinds) > 0);
    }
}
