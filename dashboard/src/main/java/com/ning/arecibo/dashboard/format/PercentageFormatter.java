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
import java.util.HashMap;

import static com.ning.arecibo.dashboard.format.DashboardFormatUtils.*;

public class PercentageFormatter extends NumberFormat {
	
	private static final NumberFormat numberFormatPercentageInstance = new PercentageFormatter(FORMAT_TYPE_PERCENTAGE);
	private static final NumberFormat numberFormatPercentageX100Instance = new PercentageFormatter(FORMAT_TYPE_PERCENTAGE_X100);
    private static final NumberFormat numberFormatPercentageXNanoInstance = new PercentageFormatter(FORMAT_TYPE_PERCENTAGE_XNANO);

    // multiply 10^-9 * 100
    private static final double NanoMultiplier = Math.pow(10.0,-7.0);

	private final String defaultFormatSpecifier;
	
	public PercentageFormatter(String formatSpecifier) {
		this.defaultFormatSpecifier = formatSpecifier;
	}	
	
	public static String formatAsPct(Object valueObj) {
		double value = (Double)valueObj;
		
		return DoubleFormatter.formatOneDecimalAndStripTrailingZeroes(value) + "%";
	}

    public static String formatAsPctX100(Object valueObj) {
        double value = (Double)valueObj * 100.0;

        return DoubleFormatter.formatOneDecimalAndStripTrailingZeroes(value) + "%";
    }

    public static String formatAsPctXNano(Object valueObj) {
		double value = (Double)valueObj * NanoMultiplier;
		
		return DoubleFormatter.formatOneDecimalAndStripTrailingZeroes(value) + "%";
	}
	
	public static String formatAsPctOfTotal(Object valueObj,String[] constituentAttrs,HashMap<String,Object> eventValuesMap) {
	    
	    if(eventValuesMap == null)
	        return "";
	    
		double value = (Double)valueObj;
		
		double total = 0.0;
		for(String attr:constituentAttrs) {
		    Double attrValue = (Double)eventValuesMap.get(attr);
		    if(attrValue != null)
		        total += attrValue;
		}
		
		if(total == 0.0) {
		    return DoubleFormatter.formatOneDecimalAndStripTrailingZeroes(value);
		}
		
		double pct = 100.0 * (value/total);
		String pctString = DoubleFormatter.formatOneDecimalAndStripTrailingZeroes(pct);
		
		return pctString + "%";
	}
	
	public static NumberFormat getNumberFormatInstance(String formatSpecifier) {
		
		if(formatSpecifier == null || formatSpecifier.equals(FORMAT_TYPE_PERCENTAGE))
			return numberFormatPercentageInstance;
		else if(formatSpecifier.equals(FORMAT_TYPE_PERCENTAGE_X100))
			return numberFormatPercentageX100Instance;
        else
            return numberFormatPercentageXNanoInstance;
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_PERCENTAGE))
			return toAppendTo.append(formatAsPct(number));
        else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_PERCENTAGE_X100))
			return toAppendTo.append(formatAsPctX100(number));
        else
            return toAppendTo.append(formatAsPctXNano(number));
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_PERCENTAGE))
			return toAppendTo.append(formatAsPct(number));
        else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_PERCENTAGE_X100))
            return toAppendTo.append(formatAsPctX100(number));
		else
			return toAppendTo.append(formatAsPctXNano(number));
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		return null;
	}
}
