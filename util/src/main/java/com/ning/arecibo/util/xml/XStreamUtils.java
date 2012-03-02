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

package com.ning.arecibo.util.xml;

import java.util.HashMap;
import java.util.Map;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;

public class XStreamUtils
{
    public static XStream getXStreamNoStringCache()
    {
        XStream xstream = new XStream();

        xstream.registerConverter(new StringConverter(new NoopMap()), 255);
        return xstream;
    }

    public static XStream getXStreamNoStringCache(HierarchicalStreamDriver driver)
    {
        XStream xstream = new XStream(driver);

        xstream.registerConverter(new StringConverter(new NoopMap()), 255);
        return xstream;
    }

    private static class NoopMap extends HashMap
    {
        public Object get(Object key)
        {
            return key;
        }

        public Object put(Object key, Object value)
        {
            return value;
        }

        public void putAll(Map m) {}

        public boolean containsKey(Object key)
        {
            return true;
        }

        public Object remove(Object key)
        {
            return key;
        }
    }
}
