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

public class MemoryFormatter extends NumberFormat {
	
	final static long OneByte = 1L;
	final static long OneBlock = OneByte << 9; // one block is 512 bytes
	final static long OneKiloByte = OneByte << 10;
	final static long OneMegaByte = OneKiloByte << 10;
	final static long OneGigaByte = OneMegaByte << 10;
	final static long OneTeraByte = OneGigaByte << 10;
	
	private final int defaultNumDecimals;
	private final String defaultFormatSpecifier;
	
	protected static final NumberFormat[] numberFormatMemoryInstances = {
		new MemoryFormatter(FORMAT_TYPE_MEMORY,1),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,2),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,3),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,4),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,5),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,6),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,7),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,8),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,9),
		new MemoryFormatter(FORMAT_TYPE_MEMORY,10)};
	
	protected static final NumberFormat[] numberFormatMemoryKbInstances = {
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,1),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,2),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,3),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,4),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,5),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,6),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,7),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,8),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,9),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_KB,10)};
	
	protected static final NumberFormat[] numberFormatMemoryMbInstances = {
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,1),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,2),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,3),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,4),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,5),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,6),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,7),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,8),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,9),
		new MemoryFormatter(FORMAT_TYPE_MEMORY_MB,10)};
	
	protected final static TickUnits standardTickUnitsMemory = createStandardTickUnits(numberFormatMemoryInstances);
	protected final static TickUnits standardTickUnitsMemoryKb = createStandardTickUnits(numberFormatMemoryKbInstances);
	protected final static TickUnits standardTickUnitsMemoryMb = createStandardTickUnits(numberFormatMemoryMbInstances);
	
	public MemoryFormatter(String formatSpecifier,int numDecimals) {
		this.defaultFormatSpecifier = formatSpecifier;
		this.defaultNumDecimals = numDecimals;
	}	
	
	public static String formatAsBytes(Object valueObj,int... numDecimals) {
	    
		double numBytes = (Double)valueObj;
		
		if(numDecimals.length > 0)
			return formatAsBytes(numBytes,numDecimals[0]);
		else
			return formatAsBytes(numBytes,1);
	}
	
	public static String formatAsBlocks(Object valueObj,int... numDecimals) {
	    
		double numBlocks = (Double)valueObj;
	    double numBytes = numBlocks * (double)OneBlock; 
		
		if(numDecimals.length > 0)
			return formatAsBytes(numBytes,numDecimals[0]);
		else
			return formatAsBytes(numBytes,1);
	}
		
		
	public static String formatAsKiloBytes(Object valueObj,int... numDecimals) {
	    
	    double numKiloBytes = (Double)valueObj;
	    double numBytes = numKiloBytes * (double)OneKiloByte; 
	    
		if(numDecimals.length > 0)
			return formatAsBytes(numBytes,numDecimals[0]);
		else
			return formatAsBytes(numBytes,1);
	}
	
	public static String formatAsMegaBytes(Object valueObj,int... numDecimals) {
	    
	    double numMegaBytes = (Double)valueObj;
	    double numBytes = numMegaBytes * (double)OneMegaByte; 
	    
		if(numDecimals.length > 0)
			return formatAsBytes(numBytes,numDecimals[0]);
		else
			return formatAsBytes(numBytes,1);
	}
	
	public static String formatAsBytes(double num,int numDecimals) {
		String units;
		
		if(num < (double)OneKiloByte) {
			units = "B" + getUnitsSuffix();
		}
		else if(num < (double)OneMegaByte) {
			num = num/(double)OneKiloByte;
			units = "KB" + getUnitsSuffix();
		}
		else if(num < (double)OneGigaByte) {
			num = num/(double)OneMegaByte;
			units = "MB" + getUnitsSuffix();
		}
		else if(num < (double)OneTeraByte) {
			num = num/(double)OneGigaByte;
			units = "GB" + getUnitsSuffix();
		}
		else {
			num = num/(double)OneTeraByte;
			units = "TB" + getUnitsSuffix();
		}
		
		String numBytesString = DoubleFormatter.formatNDecimalsAndStripTrailingZeroes(num,numDecimals);
		
		return numBytesString + " " + units;
	}
	
	protected static String getUnitsSuffix() {
		return "";
	}
	
	public static NumberFormat getNumberFormatInstance(String formatSpecifier,int numDecimals) {
		
		
		NumberFormat[] formatInstances;
		
		if(formatSpecifier == null || formatSpecifier.equals(FORMAT_TYPE_MEMORY))
			formatInstances = numberFormatMemoryInstances;
		else if(formatSpecifier.equals(FORMAT_TYPE_MEMORY_KB))
			formatInstances = numberFormatMemoryKbInstances;
		else if(formatSpecifier.equals(FORMAT_TYPE_MEMORY_MB))
			formatInstances = numberFormatMemoryMbInstances;
		else
			formatInstances = numberFormatMemoryInstances;
		
		
		if(numDecimals >= 0 && numDecimals < formatInstances.length)
			return formatInstances[numDecimals];
		
		return formatInstances[0];
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_MEMORY))
			return toAppendTo.append(formatAsBytes(number,this.defaultNumDecimals));
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_MEMORY_KB))
			return toAppendTo.append(formatAsKiloBytes(number,this.defaultNumDecimals));
		else if(this.defaultFormatSpecifier.equals(FORMAT_TYPE_MEMORY_MB))
			return toAppendTo.append(formatAsMegaBytes(number,this.defaultNumDecimals));
		else
			return toAppendTo.append(formatAsBytes(number,this.defaultNumDecimals));
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		
		return toAppendTo.append(formatAsBytes(number,this.defaultNumDecimals));
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		// unimplemented
		return null;
	}
	
	public static TickUnits getStandardTickUnits(String formatSpecifier) {
		if(formatSpecifier.equals(FORMAT_TYPE_MEMORY_KB))
			return standardTickUnitsMemoryKb;
		else if(formatSpecifier.equals(FORMAT_TYPE_MEMORY_MB))
			return standardTickUnitsMemoryMb;
		else
			return standardTickUnitsMemory;
	}
	
	public static Double getAutoRangeMinimumSize(String formatSpecifier) {
		return 1.0;
	}
	
    protected static TickUnits createStandardTickUnits(NumberFormat[] numberFormatInstances) {
    	// this is modified from the jfreechart source
    	
    	TickUnits units = new TickUnits();
    	NumberFormat df2 = numberFormatInstances[1];
    	
    	// we can add the units in any order, the TickUnits collection will
    	// sort them...
    	units.add(new NumberTickUnit(Math.pow(2,0), df2));
    	units.add(new NumberTickUnit(Math.pow(2,1), df2));
    	units.add(new NumberTickUnit(Math.pow(2,2), df2));
    	units.add(new NumberTickUnit(Math.pow(2,3), df2));
    	units.add(new NumberTickUnit(Math.pow(2,4), df2));
    	units.add(new NumberTickUnit(Math.pow(2,5), df2));
    	units.add(new NumberTickUnit(Math.pow(2,6), df2));
    	units.add(new NumberTickUnit(Math.pow(2,7), df2));
    	units.add(new NumberTickUnit(Math.pow(2,8), df2));
    	units.add(new NumberTickUnit(Math.pow(2,9), df2));
    	units.add(new NumberTickUnit(Math.pow(2,10), df2));
    	units.add(new NumberTickUnit(Math.pow(2,11), df2));
    	units.add(new NumberTickUnit(Math.pow(2,12), df2));
    	units.add(new NumberTickUnit(Math.pow(2,13), df2));
    	units.add(new NumberTickUnit(Math.pow(2,14), df2));
    	units.add(new NumberTickUnit(Math.pow(2,15), df2));
    	units.add(new NumberTickUnit(Math.pow(2,16), df2));
    	units.add(new NumberTickUnit(Math.pow(2,17), df2));
    	units.add(new NumberTickUnit(Math.pow(2,18), df2));
    	units.add(new NumberTickUnit(Math.pow(2,19), df2));
    	units.add(new NumberTickUnit(Math.pow(2,20), df2));
    	units.add(new NumberTickUnit(Math.pow(2,21), df2));
    	units.add(new NumberTickUnit(Math.pow(2,22), df2));
    	units.add(new NumberTickUnit(Math.pow(2,23), df2));
    	units.add(new NumberTickUnit(Math.pow(2,24), df2));
    	units.add(new NumberTickUnit(Math.pow(2,25), df2));
    	units.add(new NumberTickUnit(Math.pow(2,26), df2));
    	units.add(new NumberTickUnit(Math.pow(2,27), df2));
    	units.add(new NumberTickUnit(Math.pow(2,28), df2));
    	units.add(new NumberTickUnit(Math.pow(2,29), df2));
    	units.add(new NumberTickUnit(Math.pow(2,30), df2));
    	units.add(new NumberTickUnit(Math.pow(2,31), df2));
    	units.add(new NumberTickUnit(Math.pow(2,33), df2));
    	units.add(new NumberTickUnit(Math.pow(2,34), df2));
    	units.add(new NumberTickUnit(Math.pow(2,35), df2));
    	units.add(new NumberTickUnit(Math.pow(2,36), df2));
    	units.add(new NumberTickUnit(Math.pow(2,37), df2));
    	units.add(new NumberTickUnit(Math.pow(2,38), df2));
    	units.add(new NumberTickUnit(Math.pow(2,39), df2));
    	units.add(new NumberTickUnit(Math.pow(2,40), df2));
    	units.add(new NumberTickUnit(Math.pow(2,41), df2));
    	units.add(new NumberTickUnit(Math.pow(2,42), df2));
    	units.add(new NumberTickUnit(Math.pow(2,43), df2));
    	units.add(new NumberTickUnit(Math.pow(2,44), df2));
    	
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,0), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,1), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,2), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,3), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,4), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,5), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,6), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,7), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,8), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,9), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,10), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,11), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,12), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,13), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,14), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,15), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,16), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,17), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,18), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,19), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,20), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,21), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,22), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,23), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,24), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,25), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,26), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,27), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,28), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,29), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,30), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,31), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,32), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,32), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,33), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,34), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,35), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,36), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,37), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,38), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,39), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,40), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,41), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,42), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,43), df2));
    	units.add(new NumberTickUnit(2.5 * Math.pow(2,44), df2));
    	
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,0), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,1), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,2), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,3), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,4), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,5), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,6), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,7), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,8), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,9), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,10), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,11), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,12), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,13), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,14), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,15), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,16), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,17), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,18), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,19), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,20), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,21), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,22), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,23), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,24), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,25), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,26), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,27), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,28), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,29), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,30), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,31), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,32), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,33), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,34), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,35), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,36), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,37), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,38), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,39), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,40), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,41), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,42), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,43), df2));
    	units.add(new NumberTickUnit(5.0 * Math.pow(2,44), df2));
    	
    	return units;
    	
    }
}
