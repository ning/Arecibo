package com.ning.arecibo.util.jdbi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import com.ning.arecibo.util.Logger;

public class EfficientBlobMapper
{
    private static final ThreadLocal<byte[]> buf = new ThreadLocal<byte[]>()
    {
        @Override
        protected byte[] initialValue()
        {
            return new byte[8096]; // equals to chunk size
        }
    };

    private static final ThreadLocal<ByteArrayOutputStream> out = new ThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(32000);
        }
    };


    private static final Logger log = Logger.getLogger(EfficientBlobMapper.class);

    private final String columnName;

    public EfficientBlobMapper(String columnName)
    {
        this.columnName = columnName;
    }

    public byte[] map(int index, ResultSet rs, StatementContext ctx) throws SQLException
    {
        Blob blob = null;
        try {
            blob = rs.getBlob(columnName);
            return getBytes(blob);
        }
        finally {
            if (blob != null) {
                blob.free();
            }
        }
    }

    private static final byte[] getBytes(Blob blob) throws SQLException
    {
        long start = System.currentTimeMillis();
        ByteArrayOutputStream o = out.get();

        if (blob.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("blob > 2G not supported");
            // limits are good.
        }

        if (blob.length() > o.size()) {
            // create a one time use stream
            o = new ByteArrayOutputStream((int)blob.length());
        }
        InputStream in = blob.getBinaryStream();
        byte[] b = buf.get();
        int read = 0;
        try {
            while ((read = in.read(b)) > 0) {
                o.write(b, 0, read);
            }
            return o.toByteArray();
        }
        catch (IOException e) {
            throw new SQLException(e.toString());
        }
        finally {
            if (log.isDebugEnabled()) {
                log.debug("blob mapper took %d ms", System.currentTimeMillis() - start);
            }
            if ( in != null ) {
                try {
                    in.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
}
