package com.ning.arecibo.dashboard.client.datahierarchy.types;

public enum DataUrlDataType {

    // an integer
    INTEGER,

    // either an org.joda.time.Duration.toString(), or the number of millis as a Long
    DURATION_OR_MILLIS,

    // either an org.joda.time.DateTime.toString(), or the time in millis since the epoch
    DATETIME_OR_MILLIS,

    // resolution request type, can be: "HIGHEST", "HIGHESTAVAIL", "LOWEST", "LOWESTAVAIL", "BEST_FIT"
    RESOLUTION_REQUEST,
}
