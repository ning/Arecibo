package com.ning.arecibo.util.timeline;

import org.skife.jdbi.v2.IDBI;

import com.google.inject.Inject;

public class TimelineDAO {

    private final IDBI dbi;

    @Inject
    public TimelineDAO(IDBI dbi) {
        this.dbi = dbi;
    }


}
