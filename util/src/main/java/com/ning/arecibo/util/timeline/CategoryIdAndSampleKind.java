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
import java.util.Collection;
import java.util.List;

public class CategoryIdAndSampleKind
{
    private final int eventCategoryId;
    private final String sampleKind;

    public CategoryIdAndSampleKind(int eventCategoryId, String sampleKind) {
        this.eventCategoryId = eventCategoryId;
        this.sampleKind = sampleKind;
    }

    public int getEventCategoryId() {
        return eventCategoryId;
    }

    public String getSampleKind() {
        return sampleKind;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (other == null || !(other instanceof CategoryIdAndSampleKind)) {
            return false;
        }
        else {
            final CategoryIdAndSampleKind typedOther = (CategoryIdAndSampleKind)other;
            return eventCategoryId == typedOther.getEventCategoryId() && sampleKind.equals(typedOther.getSampleKind());
        }
    }

    @Override
    public int hashCode()
    {
        return eventCategoryId ^ sampleKind.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("EventCategoryIdAndSampleKind(eventCategoryId %d, sampleKind %s)", eventCategoryId, sampleKind);
    }


    public static List<String> extractSampleKinds(final Collection<CategoryIdAndSampleKind> categoryIdsAndSampleKinds)
    {
        final List<String> sampleKinds = new ArrayList<String>();
        for (CategoryIdAndSampleKind categoryIdAndSampleKind : categoryIdsAndSampleKinds) {
            sampleKinds.add(categoryIdAndSampleKind.getSampleKind());
        }
        return sampleKinds;
    }
}
