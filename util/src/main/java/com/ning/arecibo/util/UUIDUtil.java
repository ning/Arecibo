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