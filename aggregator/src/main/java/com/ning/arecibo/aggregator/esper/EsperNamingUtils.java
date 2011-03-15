package com.ning.arecibo.aggregator.esper;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

public class EsperNamingUtils {

    // something unobtrusive
    final private static String ESCAPE_SUFFIX = "_";
    final private static char ESCAPE_CHAR = '_';

    // this list pulled from Esper 3.1.0 documentation:
    //  http://esper.codehaus.org/esper-3.1.0/doc/reference/en/html/appendix_keywords.html
    final private static String[] reservedWords = {
            "all",
            "and",
            "as",
            "at",
            "asc",
            "avedev",
            "avg",
            "between",
            "by",
            "case",
            "cast",
            "coalesce",
            "count",
            "create",
            "current_timestamp",
            "day",
            "days",
            "delete",
            "desc",
            "distinct",
            "else",
            "end",
            "escape",
            "events",
            "every",
            "exists",
            "false",
            "first",
            "from",
            "full",
            "group",
            "having",
            "hour",
            "hours",
            "in",
            "inner",
            "insert",
            "instanceof",
            "into",
            "irstream",
            "is",
            "istream",
            "join",
            "last",
            "lastweekday",
            "left",
            "limit",
            "like",
            "max",
            "median",
            "metadatasql",
            "min",
            "minute",
            "minutes",
            "mesc",
            "millisecond",
            "milliseconds",
            "not",
            "null",
            "offset",
            "on",
            "or",
            "order",
            "outer",
            "output",
            "pattern",
            "prev",
            "prior",
            "regexp",
            "retain-union",
            "retain-intersection",
            "right",
            "rstream",
            "sec",
            "second",
            "seconds",
            "select",
            "set",
            "snapshot",
            "sql",
            "stddev",
            "sum",
            "then",
            "true",
            "unidirectional",
            "until",
            "variable",
            "weekday",
            "when",
            "where",
            "window"
    };

    // use a TreeSet for fast log(n) lookup
    final static TreeSet<String> reservedWordSet = new TreeSet<String>();
    static {
        reservedWordSet.addAll(Arrays.asList(reservedWords));
    }

    public static boolean isWordLegalEsperName(String word) {
        return !(isReservedWord(word) || doesWordContainIllegalChars(word));
    }

    public static String checkWordIsLegalEsperName(String word) {
        if(doesWordContainIllegalChars(word))
            word = escapeIllegalChars(word);

        if(isReservedWord(word)) {
            word = escapeReservedWord(word);
        }

        return word;
    }

    public static int checkWordsAreLegalEsperNamesInMapKeys(Map<String,Object> map) {

        // return count of keys updated
        ArrayList<String> keysToEscape = null;
        for(String key:map.keySet()) {
            if(!isWordLegalEsperName(key)) {
                if(keysToEscape == null)
                    keysToEscape = new ArrayList<String>();
                keysToEscape.add(key);
            }
        }

        if(keysToEscape != null) {
            for(String key:keysToEscape) {
                String escapedKey = checkWordIsLegalEsperName(key);
                map.put(escapedKey,map.remove(key));
            }

            return keysToEscape.size();
        }

        return 0;
    }

    private static boolean isReservedWord(String word) {
        // esper reserved words apparently are case insensitive
        return reservedWordSet.contains(word.toLowerCase());
    }

    private static String escapeReservedWord(String word) {

        // escape, preserving case sensitive version of input word
        return word + ESCAPE_SUFFIX;
    }

    private static boolean doesWordContainIllegalChars(String word) {

        // first char can't be in '0-9'
        char firstChar = word.charAt(0);
        if(firstChar >= '0' && firstChar <= '9')
            return true;

        // only allow 'a-z','A-Z','0-9','_'
        for(int i=0;i<word.length();i++) {

            char ch = word.charAt(i);

            if(ch >= 'a' && ch <= 'z')
                continue;

            if(ch >= 'A' && ch <= 'Z')
                continue;

            if(ch >= '0' && ch <= '9')
                continue;

            if(ch == '_')
                continue;

            return true;
        }

        return false;
    }

    private static String escapeIllegalChars(String word) {

        StringBuilder sb = new StringBuilder(word);

        // first char can't be in '0-9'
        char firstChar = sb.charAt(0);
        if(firstChar >= '0' && firstChar <= '9') {
            sb.insert(0,ESCAPE_CHAR);
        }

        // only allow 'a-z','A-Z','0-9','_'
        for(int i=0;i<sb.length();i++) {

            char ch = sb.charAt(i);

            if(ch >= 'a' && ch <= 'z')
                continue;

            if(ch >= 'A' && ch <= 'Z')
                continue;

            if(ch >= '0' && ch <= '9')
                continue;

            if(ch == '_')
                continue;

            sb.setCharAt(i,ESCAPE_CHAR);
        }

        return sb.toString();
    }
}
