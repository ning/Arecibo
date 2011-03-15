package com.ning.arecibo.alert.objects;

import java.util.Comparator;

public class ConfigurableObjectComparator implements Comparator<ConfigurableObject>
{
    private final static ConfigurableObjectComparator instance = new ConfigurableObjectComparator();
    
    public static ConfigurableObjectComparator getInstance() {
        return instance;
    }
    
    public int compare(ConfigurableObject tm1, ConfigurableObject tm2) {
        String id1 = tm1.getLabel();
        String id2 = tm2.getLabel();
        
        return id1.compareTo(id2);
    }
    
    public boolean equals(ConfigurableObject tm1, ConfigurableObject tm2) {
        String id1 = tm1.getLabel();
        String id2 = tm2.getLabel();
        
        return id1.equals(id2);
    }
}
