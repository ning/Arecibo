package com.ning.arecibo.dashboard.data.v1;

import com.ning.arecibo.dashboard.client.datahierarchy.types.DataHierarchyTokenType;

public enum DataHierarchyEventSuffixByToken {
    _TYPE(DataHierarchyTokenType.type,"_type"),
    _PATH(DataHierarchyTokenType.path,"_path"),
    _HOST(DataHierarchyTokenType.host,"_host");

    private final DataHierarchyTokenType tokenType;
    private final String suffix;

    private DataHierarchyEventSuffixByToken(DataHierarchyTokenType tokenType,String suffix) {
        this.tokenType = tokenType;
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public static String getSuffixForTokenType(DataHierarchyTokenType tokenType) {
        for(DataHierarchyEventSuffixByToken dhesbt:DataHierarchyEventSuffixByToken.values()) {
            if(dhesbt.tokenType.equals(tokenType)) {
                return dhesbt.suffix;
            }
        }

        return null;
    }
}
