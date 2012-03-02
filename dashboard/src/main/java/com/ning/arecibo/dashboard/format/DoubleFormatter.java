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
