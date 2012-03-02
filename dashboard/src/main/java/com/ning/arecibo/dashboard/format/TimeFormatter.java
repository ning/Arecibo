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

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;

import static com.ning.arecibo.dashboard.format.DashboardFormatUtils.*;

public class TimeFormatter extends NumberFormat {

	final static long OneMillisecond = 1L;
	final static long OneSecond = 1000L * OneMillisecond;
	final static long OneMinute = 60L * OneSecond;
	final static long OneHour = 60L * OneMinute;
	final static long OneDay = 24L * OneHour;
    final static long MicrosPerMillisecond = 1000L;
    final static long NanosPerMillisecond = 1000000L;

	final static double OneMillisecondDbl = (double)OneMillisecond;
	final static double OneSecondDbl = (double)OneSecond;
	final static double OneMinuteDbl = (double)OneMinute;
	final static double OneHourDbl = (double)OneHour;
	final static double OneDayDbl = (double)OneDay;
    final static double NanosPerMillisecondDbl = (double)NanosPerMillisecond;
    final static double MicrosPerMillisecondDbl = (double)MicrosPerMillisecond;
    final static double OneMicrosecondDbl = OneMillisecondDbl / MicrosPerMillisecond;
    final static double OneNanosecondDbl = OneMillisecondDbl / NanosPerMillisecond;

    private static NumberFormat numberFormatNsInstance = new TimeFormatter(FORMAT_TYPE_TIME_NS);
	private static NumberFormat numberFormatMsInstance = new TimeFormatter(FORMAT_TYPE_TIME_MS);
	private static NumberFormat numberFormatSecInstance = new TimeFormatter(FORMAT_TYPE_TIME_SEC);
	private static NumberFormat numberFormatHrInstance = new TimeFormatter(FORMAT_TYPE_TIME_HR);
	
	private static TickUnits standardTickUnitsNs = createStandardTickUnitsMs(numberFormatNsInstance);
    private static TickUnits standardTickUnitsMs = createStandardTickUnitsMs(numberFormatMsInstance);
	private static TickUnits standardTickUnitsSec = createStandardTickUnitsSec(numberFormatSecInstance);
	private static TickUnits standardTickUnitsHr = createStandardTickUnitsHr(numberFormatHrInstance);
	
	private final String defaultFormatSpecifier;
	
	public TimeFormatter(String formatSpecifier) {
		this.defaultFormatSpecifier = formatSpecifier;
	}
    
    public static String formatAsNanoseconds(Object valueObj) {

        double numNanoseconds = (Double)valueObj;
        return formatAsMilliseconds(numNanoseconds * OneNanosecondDbl);
    }
	
	public static String formatAsMilliseconds(Object valueObj) {

		double numMilliseconds = (Double)valueObj;
        return formatAsMilliseconds(numMilliseconds);
	}
		
	public static String formatAsSeconds(Object valueObj) {
	    
	    double numSeconds = (Double)valueObj;
	    double numMilliseconds = numSeconds * OneSecondDbl;
	    return formatAsMilliseconds(numMilliseconds);
	}
	
	public static String formatAsHours(Object valueObj) {
	    
	    double numHours = (Double)valueObj;
	    double numMilliseconds = numHours * OneHourDbl;
	    return formatAsMilliseconds(numMilliseconds);
	}
	
	public static String formatAsMilliseconds(double num) {
		String units;

        if(num == 0.0) {
            units = "";
        }
		else if(Math.abs(num) < OneMicrosecondDbl) {
            num = num/OneNanosecondDbl;
			units = " ns";
		}
        else if(Math.abs(num) < OneMillisecondDbl) {
            num = num/OneMicrosecondDbl;
            units = " us";
        }
        else if(Math.abs(num) < OneSecondDbl) {
            units = " ms";
        }
		else if(Math.abs(num) < OneMinuteDbl) {
			num = num/OneSecondDbl;
			units = " secs";
		}
		else if(Math.abs(num) < OneHourDbl) {
			num = num/OneMinuteDbl;
			units = " mins";
		}
		else if(Math.abs(num) < OneDayDbl) {
			num = num/OneHourDbl;
			units = " hrs";
		}
		else {
			num = num/OneDayDbl;
			units = " days";
		}
		
		String numString = DoubleFormatter.formatNDecimalsAndStripTrailingZeroes(num,4);
		
		return numString + units;
	}

	public static NumberFormat getNumberFormatInstance(String formatSpecifier) {
		
		if(formatSpecifier == null || formatSpecifier.equals(FORMAT_TYPE_TIME_MS))
			return numberFormatMsInstance;
        else if(formatSpecifier.equals(FORMAT_TYPE_TIME_NS))
            return numberFormatNsInstance;
		else if(formatSpecifier.equals(FORMAT_TYPE_TIME_SEC))
			return numberFormatSecInstance;
		else if(formatSpecifier.equals(FORMAT_TYPE_TIME_HR))
			return numberFormatHrInstance;
		else
			return numberFormatMsInstance;
		
	}
	
	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_MS)) {
			return toAppendTo.append(formatAsMilliseconds(number));
		}
        else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_NS)) {
            return toAppendTo.append(formatAsNanoseconds(number));
        }
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_SEC)) {
			return toAppendTo.append(formatAsSeconds(number));
		}
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_HR)) {
			return toAppendTo.append(formatAsHours(number));
		}
		else {
			return toAppendTo.append(formatAsMilliseconds(number));
		}
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_MS)) {
			return toAppendTo.append(formatAsMilliseconds(number));
		}
        else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_NS)) {
            return toAppendTo.append(formatAsNanoseconds(number));
        }
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_SEC)) {
			return toAppendTo.append(formatAsSeconds(number));
		}
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_TIME_HR)) {
			return toAppendTo.append(formatAsHours(number));
		}
		else {
			return toAppendTo.append(formatAsMilliseconds(number));
		}
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		return null;
	}
	
	public static TickUnits getStandardTickUnits(String formatSpecifier) {
		if(formatSpecifier == null || formatSpecifier.equals(FORMAT_TYPE_TIME_MS))
			return standardTickUnitsMs;
        else if(formatSpecifier.equals(FORMAT_TYPE_TIME_NS))
            return standardTickUnitsNs;
		else if(formatSpecifier.equals(FORMAT_TYPE_TIME_SEC))
			return standardTickUnitsSec;
		else
			return standardTickUnitsHr;
	}

    private static TickUnits createStandardTickUnitsNanoSec(NumberFormat nf) {
        // this is modified from the jfreechart source

        TickUnits units = new TickUnits();

        // we can add the units in any order, the TickUnits collection will sort them
        units.add(new NumberTickUnit(OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(10.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(100.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(10.0 * OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(10.0 * OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(10.0 * OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(OneDayDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(10.0 * OneDayDbl / OneNanosecondDbl, nf));

        units.add(new NumberTickUnit(0.25 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 10.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 100.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 10.0 * OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 10.0 * OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 10.0 * OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * OneDayDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.25 * 10.0 * OneDayDbl / OneNanosecondDbl, nf));

        units.add(new NumberTickUnit(0.5 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 10.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 100.0 * OneMillisecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 10.0 * OneSecondDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 10.0 * OneMinuteDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 10.0 * OneHourDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * OneDayDbl / OneNanosecondDbl, nf));
        units.add(new NumberTickUnit(0.5 * 10.0 * OneDayDbl / OneNanosecondDbl, nf));

        return units;

    }

    private static TickUnits createStandardTickUnitsMs(NumberFormat nf) {
    	// this is modified from the jfreechart source
    	
    	TickUnits units = new TickUnits();

    	// we can add the units in any order, the TickUnits collection will sort them
    	units.add(new NumberTickUnit(OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(100.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneSecondDbl, nf));
    	units.add(new NumberTickUnit(OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneHourDbl, nf));
    	units.add(new NumberTickUnit(OneDayDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneDayDbl, nf));
    	
    	units.add(new NumberTickUnit(0.25 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 100.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneDayDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneDayDbl, nf));
    	
    	units.add(new NumberTickUnit(0.5 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 100.0 * OneMillisecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMinuteDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneDayDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneDayDbl, nf));
    	
    	return units;
    	
    }	
	
    private static TickUnits createStandardTickUnitsSec(NumberFormat nf) {
    	// this is modified from the jfreechart source
    	
    	TickUnits units = new TickUnits();

    	// we can add the units in any order, the TickUnits collection will sort them
    	units.add(new NumberTickUnit(OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(100.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(OneDayDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneDayDbl/OneSecondDbl, nf));
    	
    	units.add(new NumberTickUnit(0.25 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 100.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneDayDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneDayDbl/OneSecondDbl, nf));
    	
    	units.add(new NumberTickUnit(0.5 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 100.0 * OneMillisecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneSecondDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMinuteDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneHourDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneDayDbl/OneSecondDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneDayDbl/OneSecondDbl, nf));
    	
    	return units;
    	
    }	
    
    private static TickUnits createStandardTickUnitsHr(NumberFormat nf) {
    	// this is modified from the jfreechart source
    	
    	TickUnits units = new TickUnits();

    	// we can add the units in any order, the TickUnits collection will sort them
    	units.add(new NumberTickUnit(OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(100.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(OneDayDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(10.0 * OneDayDbl/OneHourDbl, nf));
    	
    	units.add(new NumberTickUnit(0.25 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 100.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * OneDayDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.25 * 10.0 * OneDayDbl/OneHourDbl, nf));
    	
    	units.add(new NumberTickUnit(0.5 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 100.0 * OneMillisecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneSecondDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneMinuteDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneHourDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * OneDayDbl/OneHourDbl, nf));
    	units.add(new NumberTickUnit(0.5 * 10.0 * OneDayDbl/OneHourDbl, nf));
    	
    	return units;
    	
    }	
}
