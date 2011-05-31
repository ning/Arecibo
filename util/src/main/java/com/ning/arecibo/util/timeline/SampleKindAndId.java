package com.ning.arecibo.util.timeline;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class SampleKindAndId {
    public static final ResultSetMapper<SampleKindAndId> sampleKindAndIdMapper = new ResultSetMapper<SampleKindAndId>() {

        @Override
        public SampleKindAndId map(int index, ResultSet r, StatementContext ctx) throws SQLException {
            return new SampleKindAndId(r.getString("sample_kind"), r.getInt("sample_kind_id"));
        }

    };

    private final String sampleKind;
    private final int sampleKindId;

    public SampleKindAndId(String sampleKind, int sampleKindId) {
        this.sampleKind = sampleKind;
        this.sampleKindId = sampleKindId;
    }

    public String getSampleKind() {
        return sampleKind;
    }

    public int getSampleKindId() {
        return sampleKindId;
    }
}
