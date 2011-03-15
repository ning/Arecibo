package com.ning.arecibo.alertmanager.utils;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;

public class DayOfWeekUtils {

    public final static String DEFAULT_DAY_OF_WEEK_FORMAT = "E";
    public final static String NONE_SELECTED = "None Selected";
    public final static int NONE_SELECTED_INDEX = -1;

    public final static int[] DaysOfTheWeek = {
                -1,
                DateTimeConstants.MONDAY,
                DateTimeConstants.TUESDAY,
                DateTimeConstants.WEDNESDAY,
                DateTimeConstants.THURSDAY,
                DateTimeConstants.FRIDAY,
                DateTimeConstants.SATURDAY,
                DateTimeConstants.SUNDAY
    };

    public final static String[] DaysOfTheWeekStrings = {
                NONE_SELECTED,
                format(DateTimeConstants.MONDAY),
                format(DateTimeConstants.TUESDAY),
                format(DateTimeConstants.WEDNESDAY),
                format(DateTimeConstants.THURSDAY),
                format(DateTimeConstants.FRIDAY),
                format(DateTimeConstants.SATURDAY),
                format(DateTimeConstants.SUNDAY)
    };

    public static String format(Integer dayOfWeek) {
        return format(dayOfWeek,DEFAULT_DAY_OF_WEEK_FORMAT);
    }

    public static String format(Integer dayOfWeek,String formatPattern) {
        if (dayOfWeek != null) {

            if(dayOfWeek == NONE_SELECTED_INDEX) {
                return null;
            }

            LocalDate ld = new LocalDate(0L, DateTimeZone.forOffsetHours(0)).withDayOfWeek(dayOfWeek);

            return DateTimeFormat.forPattern(formatPattern).print(ld);
        }
        else
            return null;
    }

    public static Integer getDayOfWeekFromString(String dowString) {

        if(dowString != null) {

            if(dowString.equals(NONE_SELECTED))
                return null;

            // simple brute force search, seems more efficient than trying parsing with Jodatime, etc.
            // assumes input string was chosen from one of the entries in DaysOfTheWeekStrings
            for(int index=0;index<DaysOfTheWeek.length;index++) {
                if(DaysOfTheWeekStrings[index].equals(dowString))
                    return DaysOfTheWeek[index];
            }
        }

        return null;
    }
}
