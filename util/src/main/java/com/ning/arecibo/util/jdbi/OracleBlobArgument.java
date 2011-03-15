package com.ning.arecibo.util.jdbi;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import oracle.jdbc.OraclePreparedStatement;
import oracle.sql.BLOB;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import com.ning.arecibo.util.Logger;

public class OracleBlobArgument implements Argument
{
    static Logger log = Logger.getLogger(OracleBlobArgument.class);

    private static ThreadLocal<List<BLOB>> localBlobs = new ThreadLocal<List<BLOB>>(){
        protected List<BLOB> initialValue()
        {
            return new ArrayList<BLOB>();
        }
    };

    public static void freeBlobs()
    {
        try {
            if ( !localBlobs.get().isEmpty() ) {
                for (BLOB blob : localBlobs.get()) {
                    try {
                        if ( blob.isTemporary() ) {
                            log.info("freeing temporary BLOB"); 
                            blob.freeTemporary();
                        }
                    }
                    catch (SQLException e) {
                    }
                }
            }
        }
        finally {
            localBlobs.get().clear();
        }
    }

    public static void addBlob(BLOB blob)
    {
        localBlobs.get().add(blob);
    }

    private final byte[] payload ;

    public OracleBlobArgument(byte[] payload)
    {
        this.payload = payload;
    }


    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        /*
        Have to cast to OraclePreparedStatement, the oracle driver does not implement the java.sql.Blob methods
        and throws AbstractMethodError.
         */

        if ( statement instanceof OraclePreparedStatement)
        {
            log.info("creating temporary BLOB");
            OraclePreparedStatement stmt = (OraclePreparedStatement) statement ;
            BLOB blob = BLOB.createTemporary(stmt.getConnection(), true, BLOB.DURATION_SESSION);
            blob.setBytes(1L, payload);
            stmt.setBLOB(position, blob);
            addBlob(blob);
        }
        else {
            statement.setBytes(position, payload);
        }
    }
}
