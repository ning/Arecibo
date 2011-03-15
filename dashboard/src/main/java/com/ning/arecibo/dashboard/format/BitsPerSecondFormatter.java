package com.ning.arecibo.dashboard.format;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;

import static com.ning.arecibo.dashboard.format.DashboardFormatUtils.*;

public class BitsPerSecondFormatter extends NumberFormat {
	
	final static long OneBit = 1L;
	final static long OneOctet = OneBit << 3;
	final static long OneKiloBit = OneBit << 10;
	final static long OneMegaBit = OneKiloBit << 10;
	final static long OneGigaBit = OneMegaBit << 10;
	final static long OneTeraBit = OneGigaBit << 10;
	
	private final int defaultNumDecimals;
	private final String defaultFormatSpecifier;
	
	protected static final NumberFormat[] numberFormatBitsOctetsInstances = {
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,1),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,2),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,3),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,4),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,5),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,6),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,7),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,8),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,9),
		new BitsPerSecondFormatter(FORMAT_TYPE_BITS_OCTETS,10)};
	
	protected final static TickUnits standardTickUnitsBitsOctets = createStandardTickUnits(numberFormatBitsOctetsInstances);
	
	public BitsPerSecondFormatter(String formatSpecifier,int numDecimals) {
		this.defaultFormatSpecifier = formatSpecifier;
		this.defaultNumDecimals = numDecimals;
	}	
	
	public static String formatAsBits(Object valueObj,int... numDecimals) {
	    
		double numBits = (Double)valueObj;
		
		if(numDecimals.length > 0)
			return formatAsBits(numBits,numDecimals[0]);
		else
			return formatAsBits(numBits,1);
	}
	
	public static String formatAsOctets(Object valueObj,int... numDecimals) {
	    
		double numOctets = (Double)valueObj;
	    double numBits = numOctets * (double)OneOctet; 
		
		if(numDecimals.length > 0)
			return formatAsBits(numBits,numDecimals[0]);
		else
			return formatAsBits(numBits,1);
	}
		
	public static String formatAsBits(double num,int numDecimals) {
		String units;
		
		if(num < (double)OneKiloBit) {
			units = "b" + getUnitsSuffix();
		}
		else if(num < (double)OneMegaBit) {
			num = num/(double)OneKiloBit;
			units = "kb" + getUnitsSuffix();
		}
		else if(num < (double)OneGigaBit) {
			num = num/(double)OneMegaBit;
			units = "mb" + getUnitsSuffix();
		}
		else if(num < (double)OneTeraBit) {
			num = num/(double)OneGigaBit;
			units = "gb" + getUnitsSuffix();
		}
		else {
			num = num/(double)OneTeraBit;
			units = "tb" + getUnitsSuffix();
		}
		
		String numBytesString = DoubleFormatter.formatNDecimalsAndStripTrailingZeroes(num,numDecimals);
		
		return numBytesString + " " + units;
	}
	
	protected static String getUnitsSuffix() {
		return "ps";
	}
	
	public static NumberFormat getNumberFormatInstance(String formatSpecifier,int numDecimals) {
		
		
		NumberFormat[] formatInstances;
		
		if(formatSpecifier == null || formatSpecifier.equals(FORMAT_TYPE_BITS_OCTETS))
			formatInstances = numberFormatBitsOctetsInstances;
		else
			// default anyway
			formatInstances = numberFormatBitsOctetsInstances;
		
		
		if(numDecimals >= 0 && numDecimals < formatInstances.length)
			return formatInstances[numDecimals];
		
		return formatInstances[0];
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		
		
		if(this.defaultFormatSpecifier == null || this.defaultFormatSpecifier.equals(FORMAT_TYPE_BITS_OCTETS))
			return toAppendTo.append(formatAsOctets(number,this.defaultNumDecimals));
		else
			// default anyway
			return toAppendTo.append(formatAsOctets(number,this.defaultNumDecimals));
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		
		return toAppendTo.append(formatAsOctets(number,this.defaultNumDecimals));
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		// unimplemented
		return null;
	}
	
	public static TickUnits getStandardTickUnits(String formatSpecifier) {
		if(formatSpecifier.equals(FORMAT_TYPE_BITS_OCTETS))
			return standardTickUnitsBitsOctets;
		else
			return null;
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
