package com.ning.arecibo.alert.confdata.enums;

public enum ManagingKeyActionType {
    
    NO_ACTION(0),
    QUIESCE(1),
    DISABLE(2);

    private final int level;

    private ManagingKeyActionType(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }
}
