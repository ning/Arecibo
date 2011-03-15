package com.ning.arecibo.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class UUIDUtil
{
    public static UUID md5UUID(String key)
    {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            byte[] d = md5.digest();

            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (d[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (d[i] & 0xff);
            }

            return new UUID(msb, lsb);
        }
        catch (NoSuchAlgorithmException e) {
            long msb = key.hashCode();
            long lsb = (msb << 8) | msb;
            return new UUID(msb, lsb);
        }
    }

    public static String md5ToString(byte[] d)
    {
        StringBuilder buf = new StringBuilder();
        for ( byte b : d ) {
            String hex = Integer.toHexString(0xff & b) ;
            if ( hex.length() == 1 ) {
                buf.append("0");
            }
            buf.append(hex);
        }
        return buf.toString();
    }
}