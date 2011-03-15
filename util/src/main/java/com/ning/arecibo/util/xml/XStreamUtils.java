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
