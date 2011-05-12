package com.ning.arecibo.util.timeline;

import java.util.List;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import com.google.inject.Inject;

public class TimelineDAO {
    private static final String PACKAGE = TimelineDAO.class.getName();

    private final IDBI dbi;

    @Inject
    public TimelineDAO(IDBI dbi) {
        this.dbi = dbi;
    }

    public List<String> getHosts() {
        return dbi.withHandle(new HandleCallback<List<String>>() {

            @Override
            public List<String> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getHosts")
                    .map(StringMapper.FIRST)
                    .list();
            }

        });

    }

    public List<SampleKindAndId> getSampleKindsAndIds() {
        return dbi.withHandle(new HandleCallback<List<SampleKindAndId>>() {

            @Override
            public List<SampleKindAndId> withHandle(Handle handle) throws Exception {
                return handle.createQuery(PACKAGE + ":getSampleKindsAndIds")
                    .map(SampleKindAndId.sampleKindAndIdMapper)
                    .list();
            }

        });

    }
}
