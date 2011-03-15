package com.ning.arecibo.dashboard.format;

import java.util.Formatter;

public class DoubleFormatter {
	
	public static String formatOneDecimalAndStripTrailingZeroes(Object value) {
		return formatNDecimalsAndStripTrailingZeroes(value,1,true);
	}
	
	public static String formatNDecimalsAndStripTrailingZeroes(Object value, int numDecimals) {
		return formatNDecimalsAndStripTrailingZeroes(value,numDecimals,true);
	}
	
	public static String formatNDecimalsAndStripTrailingZeroes(Object value, int numDecimals, boolean okToAddHtml) {
	    
	    if(!(value instanceof Number)) 
	        return "NaN";

        double v = ((Number)value).doubleValue();

        StringBuilder sb = new StringBuilder();
	    
	    if(Math.abs(v) >= 0.1 || v == 0.0) {
			Formatter formatter = new Formatter(sb);
			formatter.format("%." + numDecimals + "f",v);
		
            removeTrailingZeroes(sb);
	    }
	    else {
    		Formatter formatter = new Formatter(sb);
    		String formatStr = "%." + numDecimals + "g";
    		formatter.format(formatStr, v);
    		int expIndex = sb.indexOf("e");

            if(expIndex == -1) {
                removeTrailingZeroes(sb);
            }
    		else {
                expIndex = removeTrailingZeroes(sb,expIndex);
                if(okToAddHtml){
                    sb.insert(expIndex+1, "<sup>");
                    sb.append("</sup>");
                }
            }
	    }
		
		return sb.toString();
	}

    private static void removeTrailingZeroes(StringBuilder sb) {
        char lastChar;
        int lastIndex = sb.length() - 1;
        int startIndex = (sb.charAt(0) == '-')?1:0;

        while(lastIndex > startIndex && ((lastChar = sb.charAt(lastIndex)) == '0' || lastChar == '.')) {
            sb.deleteCharAt(lastIndex);

            if(lastChar == '.')
                break;

            lastIndex--;
        }
    }

    private static int removeTrailingZeroes(StringBuilder sb,int expIndex) {
        char lastChar;
        int lastIndex = expIndex - 1;
        int startIndex = (sb.charAt(0) == '-')?1:0;

        while(lastIndex > startIndex && ((lastChar = sb.charAt(lastIndex)) == '0' || lastChar == '.')) {
            sb.deleteCharAt(lastIndex);

            if(lastChar == '.')
                break;

            lastIndex--;
        }

        return lastIndex;
    }
}
