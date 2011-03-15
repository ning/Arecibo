package com.ning.arecibo.dashboard.format;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;

public class NumberRangeFormatter extends NumberFormat {
	
	final static long One = 1L;
	final static long OneKilo = One * 1000L;
	final static long TenKilo = OneKilo * 10L;
	final static long OneMega = TenKilo * 100L;
	final static long OneGiga = OneMega * 1000L;
	final static long OneTera = OneGiga * 1000L;
	
	protected final int defaultNumDecimals;
	protected final boolean okToAddHtml;
	
	protected static String unitsAppendage = "";
	
	protected static final NumberFormat[] numberFormatInstances = {
		new NumberRangeFormatter(1),
		new NumberRangeFormatter(2),
		new NumberRangeFormatter(3),
		new NumberRangeFormatter(4),
		new NumberRangeFormatter(5),
		new NumberRangeFormatter(6),
		new NumberRangeFormatter(7),
		new NumberRangeFormatter(8),
		new NumberRangeFormatter(9),
		new NumberRangeFormatter(10)};
	
	protected final static TickUnits standardTickUnits = createStandardTickUnits();
	
	public NumberRangeFormatter() {
		this(1);
	}
	
	public NumberRangeFormatter(int numDecimals) {
		this.defaultNumDecimals = numDecimals;
		this.okToAddHtml = false;
	}
	
	public NumberRangeFormatter(int numDecimals,boolean okToAddHtml) {
		this.defaultNumDecimals = numDecimals;
		this.okToAddHtml = okToAddHtml;
	}
	
	public static String formatAsOnes(Object valueObj) {
	    
        if(!(valueObj instanceof Number)) 
            return "NaN";

        double num = ((Number)valueObj).doubleValue(); 
		return formatAsOnes(num);
	}
		
	public static String formatAsOnes(double num) {
		return formatAsOnes(num,1,true);
	}
	
	public static String formatAsOnes(double num,int numDecimals,boolean okToAddHtml) {
		String units;
		
		if(Math.abs(num) < (double)TenKilo) {
			units = "";
		}
		else if(Math.abs(num) < (double)OneMega) {
			num = num/(double)OneKilo;
			units = " K";
		}
		else if(Math.abs(num) < (double)OneGiga) {
			num = num/(double)OneMega;
			units = " M";
		}
		else if(Math.abs(num) < (double)OneTera) {
			num = num/(double)OneGiga;
			units = " G";
		}
		else {
			num = num/(double)OneTera;
			units = " T";
		}
		
		String numString = DoubleFormatter.formatNDecimalsAndStripTrailingZeroes(num,numDecimals,okToAddHtml);
		
		return numString + units + unitsAppendage;
	}
	
	public static NumberFormat getNumberFormatInstance(int numDecimals) {
		if(numDecimals >= 0 && numDecimals < numberFormatInstances.length)
			return numberFormatInstances[numDecimals];
		
		return numberFormatInstances[0];
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
		
		return toAppendTo.append(formatAsOnes(number,this.defaultNumDecimals,this.okToAddHtml));
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
		
		return toAppendTo.append(formatAsOnes(number,this.defaultNumDecimals,this.okToAddHtml));
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		// unimplemented
		return null;
	}
	
	public static TickUnits getStandardTickUnits() {
		return standardTickUnits;
	}
	
    protected static TickUnits createStandardTickUnits() {
    	// this is modified from the jfreechart source
    	
    	TickUnits units = new TickUnits();
    	NumberFormat df2 = new NumberRangeFormatter(2,false);
    	
    	// we can add the units in any order, the TickUnits collection will
    	// sort them...
    	units.add(new NumberTickUnit(0.0000001, df2));
    	units.add(new NumberTickUnit(0.000001, df2));
    	units.add(new NumberTickUnit(0.00001, df2));
    	units.add(new NumberTickUnit(0.0001, df2));
    	units.add(new NumberTickUnit(0.001, df2));
    	units.add(new NumberTickUnit(0.01, df2));
    	units.add(new NumberTickUnit(0.1, df2));
    	units.add(new NumberTickUnit(1L, df2));
    	units.add(new NumberTickUnit(10L, df2));
    	units.add(new NumberTickUnit(100L, df2));
    	units.add(new NumberTickUnit(1000L, df2));
    	units.add(new NumberTickUnit(10000L, df2));
    	units.add(new NumberTickUnit(100000L, df2));
    	units.add(new NumberTickUnit(1000000L, df2));
    	units.add(new NumberTickUnit(10000000L, df2));
    	units.add(new NumberTickUnit(100000000L, df2));
    	units.add(new NumberTickUnit(1000000000L, df2));
    	units.add(new NumberTickUnit(10000000000L, df2));
    	units.add(new NumberTickUnit(100000000000L, df2));
    	
    	units.add(new NumberTickUnit(0.00000025, df2));
    	units.add(new NumberTickUnit(0.0000025, df2));
    	units.add(new NumberTickUnit(0.000025, df2));
    	units.add(new NumberTickUnit(0.00025, df2));
    	units.add(new NumberTickUnit(0.0025, df2));
    	units.add(new NumberTickUnit(0.025, df2));
    	units.add(new NumberTickUnit(0.25, df2));
    	units.add(new NumberTickUnit(2.5, df2));
    	units.add(new NumberTickUnit(25L, df2));
    	units.add(new NumberTickUnit(250L, df2));
    	units.add(new NumberTickUnit(2500L, df2));
    	units.add(new NumberTickUnit(25000L, df2));
    	units.add(new NumberTickUnit(250000L, df2));
    	units.add(new NumberTickUnit(2500000L, df2));
    	units.add(new NumberTickUnit(25000000L, df2));
    	units.add(new NumberTickUnit(250000000L, df2));
    	units.add(new NumberTickUnit(2500000000L, df2));
    	units.add(new NumberTickUnit(25000000000L, df2));
    	units.add(new NumberTickUnit(250000000000L, df2));
    	
    	units.add(new NumberTickUnit(0.0000005, df2));
    	units.add(new NumberTickUnit(0.000005, df2));
    	units.add(new NumberTickUnit(0.00005, df2));
    	units.add(new NumberTickUnit(0.0005, df2));
    	units.add(new NumberTickUnit(0.005, df2));
    	units.add(new NumberTickUnit(0.05, df2));
    	units.add(new NumberTickUnit(0.5, df2));
    	units.add(new NumberTickUnit(5L, df2));
    	units.add(new NumberTickUnit(50L, df2));
    	units.add(new NumberTickUnit(500L, df2));
    	units.add(new NumberTickUnit(5000L, df2));
    	units.add(new NumberTickUnit(50000L, df2));
    	units.add(new NumberTickUnit(500000L, df2));
    	units.add(new NumberTickUnit(5000000L, df2));
    	units.add(new NumberTickUnit(50000000L, df2));
    	units.add(new NumberTickUnit(500000000L, df2));
    	units.add(new NumberTickUnit(5000000000L, df2));
    	units.add(new NumberTickUnit(50000000000L, df2));
    	units.add(new NumberTickUnit(500000000000L, df2));
    	
    	return units;
    	
    }
}
