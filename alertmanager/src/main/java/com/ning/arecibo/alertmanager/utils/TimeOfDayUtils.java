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
