package com.ning.arecibo.collector.contentstore;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Update;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.ZipUtils;
import com.ning.arecibo.util.jdbi.EfficientBlobMapper;
import com.ning.arecibo.util.jdbi.OracleBlobArgument;

public class DbEntryUtil
{
    public enum EntryMode
    {
        NOT_COMPRESSED("n"),
        ZIP("z"),
        GZIP("g");

        private final String mode;

        private EntryMode(String code)
        {
            this.mode = code;
        }

        public boolean isCompressed()
        {
            return !mode.equals(NOT_COMPRESSED.mode);
        }

        public static final EntryMode fromString(String m)
        {
            if (NOT_COMPRESSED.mode.equalsIgnoreCase(m)) {
                return NOT_COMPRESSED;
            }
            else if(ZIP.mode.equalsIgnoreCase(m)) {
                return ZIP;
            }
            else if(GZIP.mode.equalsIgnoreCase(m)) {
                return GZIP;
            }
            throw new IllegalArgumentException("invalid mode value");
        }

        public String toString()
        {
            return mode;
        }
    }

    public static final String ENTRY_COLUMN_NAME = "entry" ;
    public static final String ID_COLUMN_NAME = "id" ;
    public static final String TYPE_COLUMN_NAME = "type";
    public static final String SHAPE_VERSION_COLUMN_NAME = "shape_version" ;
    public static final String ENTRY_PART_1_COLUMN_NAME = "entry_part_1" ;
    public static final String ENTRY_PART_2_COLUMN_NAME = "entry_part_2" ;
    public static final String ENTRY_MODE_COLUMN_NAME = "entry_mode" ;
    public static final String BLOB_ID_COLUMN_NAME = "blob_id" ;
    public static final String BLOB_SIZE_COLUMN_NAME = "blob_size" ;
    public static final String IS_PRIVATE_COLUMN_NAME = "is_private";

    private static final Logger log = Logger.getLogger(DbEntryUtil.class);
    private static final EntryMode globalMode = EntryMode.valueOf(System.getProperty("xn.db.entry.compression", "GZIP"));
    public static final String UTF8 = "UTF-8";
    private static EfficientBlobMapper mapper = new EfficientBlobMapper(ENTRY_COLUMN_NAME);

    public static int bindAndExecute(Update stmt, String xml)
    {
        try {
            bind(stmt, xml);
            return stmt.execute();
        }
        finally
        {
            OracleBlobArgument.freeBlobs();
        }
    }

    public static void bind(SQLStatement<?> stmt, String xml)
    {
        try {
            byte uncompressed[] = xml.getBytes(UTF8);
            byte compressed[] = null;

            if (EntryMode.ZIP.equals(globalMode)) {
                compressed = ZipUtils.zip(uncompressed);
            }
            else if (EntryMode.GZIP.equals(globalMode)) {
                compressed = ZipUtils.gzip(uncompressed);
            }
            _bind(stmt, compressed == null ? uncompressed : compressed);
        }
        catch (IOException e) {
            // should not happen
            throw new RuntimeException(e);
        }
    }

    private static void _bind(SQLStatement<?> stmt, byte[] source)
    {
        stmt.bind(ENTRY_MODE_COLUMN_NAME, globalMode.toString());

        if (source.length <= 2000) {
            stmt.bind(ENTRY_PART_1_COLUMN_NAME, source);
            stmt.bindNull(ENTRY_PART_2_COLUMN_NAME, Types.VARBINARY);
            stmt.bindNull(ENTRY_COLUMN_NAME, Types.BLOB);
        }
        else if (source.length <= 4000) {
            int delta = source.length - 2000;
            byte buf1[] = new byte[2000];
            byte buf2[] = new byte[delta];

            System.arraycopy(source, 0, buf1, 0, 2000);
            System.arraycopy(source, 2000, buf2, 0, delta);

            stmt.bind(ENTRY_PART_1_COLUMN_NAME, buf1);
            stmt.bind(ENTRY_PART_2_COLUMN_NAME, buf2);
            stmt.bindNull(ENTRY_COLUMN_NAME, Types.BLOB);
        }
        else {
            log.debug("storing entry xml as blob, size = %d", source.length);
            stmt.bindNull(ENTRY_PART_1_COLUMN_NAME, Types.VARBINARY);
            stmt.bindNull(ENTRY_PART_2_COLUMN_NAME, Types.VARBINARY);
            stmt.bind(ENTRY_COLUMN_NAME, new OracleBlobArgument(source));
        }
    }

    public static String getEntryFromRow(ResultSet rs) throws SQLException, IOException
    {
        String modeStr = rs.getString(ENTRY_MODE_COLUMN_NAME);
        EntryMode mode = null;

        if (modeStr == null) {
            throw new IllegalArgumentException("invalid mode");
        }
        else {
            mode = EntryMode.fromString(modeStr);
        }

        byte[] buf1 = rs.getBytes(ENTRY_PART_1_COLUMN_NAME);
        byte[] buf2 = rs.getBytes(ENTRY_PART_2_COLUMN_NAME);

        if (buf1 != null && buf2 != null) { // < 4000
            byte[] b = new byte[buf1.length + buf2.length];
            System.arraycopy(buf1, 0, b, 0, buf1.length);
            System.arraycopy(buf2, 0, b, buf1.length, buf2.length);
            try {
                return new String(decompress(b, mode), UTF8);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        else if (buf1 != null) // < 2000
        {
            try {
                return new String(decompress(buf1, mode), UTF8);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            log.debug("retrieving entry xml from blob entry column");

            byte[] source = mapper.map(0, rs, null);
            try {
                return new String(decompress(source, mode), UTF8);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] decompress(byte[] b, EntryMode mode) throws IOException
    {
        if(EntryMode.GZIP.equals(mode)){
            return ZipUtils.gunzip(b);
        }
        else if(EntryMode.ZIP.equals(mode)) {
            return ZipUtils.unzip(b);
        }
        return b;
    }
}