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
