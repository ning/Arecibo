package com.ning.arecibo.dashboard.format;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class PerSecondFormatter extends NumberFormat {
    
    private final static String SLASH_SECOND = "/sec";
    private final static String PER_SECOND = "per sec";
    private final static String BITS_PS = "ps";
	
	public static String formatAsGenericPerSecond(Object valueObj) {
		double value = (Double)valueObj;
		
		return NumberRangeFormatter.formatAsOnes(value) + " " + PER_SECOND;
	}
	
	public static String addPerSecondSuffix(String prefix) {
	    if(Character.isDigit(prefix.charAt(prefix.length()-1))) {
	        return prefix + " " + PER_SECOND;
	    }
	    else if(prefix.charAt(prefix.length()-1) == 'b') {
	        return prefix + BITS_PS;
	    }
	    else {
	        return prefix + SLASH_SECOND;
	    }
	}

	@Override
	public StringBuffer format(double number, StringBuffer toAppendTo,
			FieldPosition pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StringBuffer format(long number, StringBuffer toAppendTo,
			FieldPosition pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number parse(String source, ParsePosition parsePosition) {
		return null;
	}
}
