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

package com.ning.arecibo.alertmanager.utils;

import org.joda.time.LocalTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class TimeOfDayUtils {
    public final static String DEFAULT_TIME_FORMAT = "HH:mm";

    public static String format(long millis) {
        return format(millis, DEFAULT_TIME_FORMAT);
    }

    public static String format(Long millis, String formatPattern) {
        if (millis != null) {
            LocalTime lt = new LocalTime(millis, DateTimeZone.forOffsetHours(0));

            return DateTimeFormat.forPattern(formatPattern).print(lt);
        }
        else
            return null;
    }
}
