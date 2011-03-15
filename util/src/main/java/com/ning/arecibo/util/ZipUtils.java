package com.ning.arecibo.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils
{
    private static final ThreadLocal<ByteArrayOutputStream> out = new ThreadLocal<ByteArrayOutputStream>()
    {
        protected ByteArrayOutputStream initialValue()
        {
            return new ByteArrayOutputStream(32000);
        }
    };

    private static final ThreadLocal<byte[]> buf = new ThreadLocal<byte[]>()
    {
        protected byte[] initialValue()
        {
            return new byte[4000];
        }
    };

    public static byte[] zip(byte[] input) throws IOException
    {
        ByteArrayOutputStream o = out.get();
        o.reset();
        ZipOutputStream z = null;
        try {
            z = new ZipOutputStream(o);
            z.putNextEntry(new ZipEntry("entry.xml"));
            z.write(input);
            z.closeEntry();
            z.flush();
        }
        finally {
            if (z != null) {
                z.close();
            }
        }
        return o.toByteArray();
    }

    public static byte[] unzip(byte[] input) throws IOException
    {
        byte b[] = buf.get();
        ByteArrayOutputStream o = out.get();
        o.reset();

        ZipInputStream z = null;
        try {
            z = new ZipInputStream(new ByteArrayInputStream(input));
            ZipEntry ze = z.getNextEntry();
            int read = 0;
            while ((read = z.read(b)) > 0) {
                o.write(b, 0, read);
            }
            o.flush();
        }
        finally {
            if (z != null) {
                z.close();
            }
        }

        return o.toByteArray();
    }

    public static byte[] gzip(byte[] input) throws IOException
    {
        ByteArrayOutputStream o = out.get();
        o.reset();
        GZIPOutputStream z = null;
        try {
            z = new GZIPOutputStream(o);
            z.write(input);
            z.finish();
            z.flush();
        }
        finally {
            if (z != null) {
                z.close();
            }
        }
        return o.toByteArray();
    }

    public static byte[] gunzip(byte[] input) throws IOException
    {
        byte b[] = buf.get();
        ByteArrayOutputStream o = out.get();
        o.reset();
        GZIPInputStream z = null;
        try {
            z = new GZIPInputStream(new ByteArrayInputStream(input));
            int read = 0;
            while ((read = z.read(b)) > 0) {
                o.write(b, 0, read);
            }
            o.flush();
        }
        finally {
            if (z != null) {
                z.close();
            }
        }
        return o.toByteArray();
    }
}