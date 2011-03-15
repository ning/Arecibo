package com.ning.arecibo.dashboard.alert.e2ez;

public enum E2EZMetricStatus {
    OK(0),
    WARN(10),
    CRITICAL(100),
    UNKNOWN(-1),
    INVESTIGATING(-1);

    final private int severityRank;

    private E2EZMetricStatus(int severityRank) {
        this.severityRank = severityRank;
    }

    public static E2EZMetricStatus parseMetricStatusFromString(String str) {
        if(str.contains(WARN.toString()))
            return WARN;
        else if(str.contains(CRITICAL.toString()))
            return CRITICAL;
        else
            return UNKNOWN;
    }

    public boolean isLessSevereThan(E2EZMetricStatus compareTo) {
        return (this.severityRank < compareTo.severityRank);
    }
}
