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

package com.ning.arecibo.dashboard.format;

import java.text.NumberFormat;

import org.jfree.chart.axis.TickUnits;

public interface DashboardGraphFormatter
{
    public String getFormattedValue(String lowerCaseEventName,String lowerCaseAttr,Object valueObj);
    public NumberFormat getNumberFormat(String lowerCaseEventName,String lowerCaseAttr);
    public TickUnits getTickUnits(String lowerCaseEventName,String lowerCaseAttr);
    public Double getAutoRangeMinimumSize(String lowerCaseEventName,String lowerCaseAttr,TickUnits tickUnits,Double minDataValue,Double maxDataValue);
}
