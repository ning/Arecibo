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
