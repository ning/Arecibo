package com.ning.arecibo.dashboard.graph;

import java.util.Comparator;

public class DashboardGraphLegendComparator implements Comparator<String>
{
    public final String HOST_DOMAIN = ".ningops.com";
    
    private final static DashboardGraphLegendComparator instance = new DashboardGraphLegendComparator();
    
    public static DashboardGraphLegendComparator getInstance() {
        return instance;
    } 
    
    public int compare(String s1,String s2) {
        
        if(s1.equals(s2))
            return 0;
        
        boolean s1LooksLikeAHost = s1.contains(HOST_DOMAIN);
        boolean s2LooksLikeAHost = s2.contains(HOST_DOMAIN);
        
        if(s1LooksLikeAHost == s2LooksLikeAHost)
            return s1.compareTo(s2);
        
        // non-hosts should rank higher than hosts
        if(s1LooksLikeAHost)
            return 1;
        else
            return -1;
    }
}
