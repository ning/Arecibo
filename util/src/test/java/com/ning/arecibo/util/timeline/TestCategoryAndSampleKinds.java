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

import com.fasterxml.jackson.databind.ObjectMapper;
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
}