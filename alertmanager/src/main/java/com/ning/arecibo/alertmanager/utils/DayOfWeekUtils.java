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

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

public class DayOfWeekUtils
{
    public static final String DEFAULT_DAY_OF_WEEK_FORMAT = "E";
    public static final String NONE_SELECTED = "None Selected";
    public static final int NONE_SELECTED_INDEX = -1;

    public static final int[] DaysOfTheWeek = {
        -1,
        DateTimeConstants.MONDAY,
        DateTimeConstants.TUESDAY,
        DateTimeConstants.WEDNESDAY,
        DateTimeConstants.THURSDAY,
        DateTimeConstants.FRIDAY,
        DateTimeConstants.SATURDAY,
        DateTimeConstants.SUNDAY
    };

    public static final String[] DaysOfTheWeekStrings = {
        NONE_SELECTED,
        format(DateTimeConstants.MONDAY),
        format(DateTimeConstants.TUESDAY),
        format(DateTimeConstants.WEDNESDAY),
        format(DateTimeConstants.THURSDAY),
        format(DateTimeConstants.FRIDAY),
        format(DateTimeConstants.SATURDAY),
        format(DateTimeConstants.SUNDAY)
    };

    public static String format(final Integer dayOfWeek)
    {
        return format(dayOfWeek, DEFAULT_DAY_OF_WEEK_FORMAT);
    }

    public static String format(final Integer dayOfWeek, final String formatPattern)
    {
        if (dayOfWeek != null) {

            if (dayOfWeek == NONE_SELECTED_INDEX) {
                return null;
            }

            final LocalDate ld = new LocalDate(0L, DateTimeZone.forOffsetHours(0)).withDayOfWeek(dayOfWeek);
            return DateTimeFormat.forPattern(formatPattern).print(ld);
        }
        else {
            return null;
        }
    }

    public static Integer getDayOfWeekFromString(final String dowString)
    {
        if (dowString != null) {

            if (dowString.equals(NONE_SELECTED)) {
                return null;
            }

            // simple brute force search, seems more efficient than trying parsing with Jodatime, etc.
            // assumes input string was chosen from one of the entries in DaysOfTheWeekStrings
            for (int index = 0; index < DaysOfTheWeek.length; index++) {
                if (DaysOfTheWeekStrings[index].equals(dowString)) {
                    return DaysOfTheWeek[index];
                }
            }
        }

        return null;
    }
}