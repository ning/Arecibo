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

package com.ning.arecibo.agent.datasource.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class JMXClient
{
    public static class MBeanDescriptor
    {
        // TODO: can we cache the Attribute objects (getAttributes) or do they have to be retrieved every time ?
        private final ObjectName objectName;
        private final MBeanInfo  mbeanInfo;
        private final String[]   attributeNames;

        public MBeanDescriptor(ObjectName objctName, MBeanInfo mbeanInfo)
        {
            this.objectName = objctName;
            this.mbeanInfo  = mbeanInfo;

            MBeanAttributeInfo[] attrInfos = mbeanInfo.getAttributes();

            this.attributeNames = new String[attrInfos.length];
            for (int idx = 0; idx < attrInfos.length; idx++) {
                this.attributeNames[idx] = attrInfos[idx].getName();
            }
        }

        public ObjectName getObjectName()
        {
            return objectName;
        }

        public MBeanInfo getMBeanInfo()
        {
            return mbeanInfo;
        }

        public String[] getAttributeNames()
        {
            return attributeNames;
        }
    }

    private JMXConnector          jmxConn;
    private MBeanServerConnection mbeanConn;

    public JMXClient(String host) throws IOException
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", host);

        try {
            JMXServiceURL urlObj = new JMXServiceURL(url);

            jmxConn   = JMXConnectorFactory.connect(urlObj);
            mbeanConn = jmxConn.getMBeanServerConnection();
        }
        catch (Exception ex) {
            if (jmxConn != null) {
                try {
                    jmxConn.close();
                    jmxConn = null;
                }
                catch (IOException innerEx) {
                    // ignored
                }
            }
            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            else {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Timeout constructor. Use JMXTimeoutConnector.connectWithTimeout() instead of JMXConnectorFactory.connect()
     */
    public JMXClient(String host, long timeout, TimeUnit unit) throws IOException
    {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s/jmxrmi", host);
        try {
            JMXServiceURL urlObj = new JMXServiceURL(url);

            jmxConn   = JMXTimeoutConnector.connectWithTimeout(urlObj, timeout, unit);
            mbeanConn = jmxConn.getMBeanServerConnection();
        }
        catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            else {
                throw new RuntimeException(ex);
            }
        }
    }


    public MBeanServerConnection getMBeanServerConnection()
    {
        return mbeanConn;
    }

    public List<MBeanDescriptor> getMBeans(String namePattern)
    {
        ArrayList<MBeanDescriptor> mbeans = new ArrayList<MBeanDescriptor>();

        try {
            Set<ObjectInstance> mbeanInstances = mbeanConn.queryMBeans(new ObjectName(namePattern), null);

            for (ObjectInstance mbeanInstance : mbeanInstances) {
                mbeans.add(getMBeanDescriptor(mbeanInstance));
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return mbeans;
    }

    public MBeanDescriptor getMBean(String name)
    {
        try {
            ObjectInstance mbeanInstance = mbeanConn.getObjectInstance(new ObjectName(name));

            return getMBeanDescriptor(mbeanInstance);
        }
        catch (InstanceNotFoundException ex) {
            return null;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private MBeanDescriptor getMBeanDescriptor(ObjectInstance mbeanInstance) throws ClassNotFoundException, InstanceNotFoundException, ReflectionException, IOException, IntrospectionException
    {
        MBeanInfo mbeanInfo = mbeanConn.getMBeanInfo(mbeanInstance.getObjectName());

        return new MBeanDescriptor(mbeanInstance.getObjectName(), mbeanInfo);
    }

    public <T> T getMBean(String name, Class<T> interfaceClass)
    {
        try {
            return getMBean(new ObjectName(name), interfaceClass);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public <T> T getMBean(ObjectName name, Class<T> interfaceClass)
    {
        try {
            return (T)MBeanServerInvocationHandler.newProxyInstance(mbeanConn, name, interfaceClass, false);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * return an MXBean for the given name string and interface class. Update of the getMBean method for MXBeans.
     * See getMXBean() below.
     *
     * Example usage (omitting exception handler):
     *     MemoryMXBean mem = this.client.getMXBean("java.lang:type=Memory", MemoryMXBean.class);
     *     long used = mem.getHeapMemoryUsage().getUsed();
     *
     * @param name object name as string
     * @param interfaceClass interface
     * @return instantiated proxy object
     */
    public <T> T getMXBean(String name, Class<T> interfaceClass) throws MalformedObjectNameException
    {
        try {
            return getMXBean(new ObjectName(name), interfaceClass);
        }
        catch (MalformedObjectNameException e) {
            throw new MalformedObjectNameException("bad ObjectName: " + name);
        }
    }


    /**
     * return the MXBean for the given ObjectName and interface class. This method use the updated 1.6
     * JMX.newMXBeanProxy() which replaces the "hideously unmemorable" MBeanServerInvocationHandler.newProxyInstance()
     *
     * See getMXbean for example usage.
     *
     * @param name object name
     * @param interfaceClass interface
     * @return instantiated proxy object
     */
    public <T> T getMXBean(ObjectName name, Class<T> interfaceClass) {
        return JMX.newMXBeanProxy(mbeanConn, name, interfaceClass);
    }


    /**
     * return an attribute value for a given MBean. Note that this method can also be used with MXBeans, which
     * return a CompositeData class. Here's an example:
     *
     *  JMXClient.MBeanDescriptor clusterMBean = client.getMBean("java.lang:type=Memory");
     *  CompositeData memoryUsage = (CompositeData) client.getAttributeValue(clusterMBean, "HeapMemoryUsage");
     *  long used = (Long) memoryUsage.get("used");
     */
    public Object getAttributeValue(MBeanDescriptor mbeanDesc, String attrName)
    {
        try {
            return mbeanConn.getAttribute(mbeanDesc.getObjectName(), attrName);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String, Object> getAttributeValues(MBeanDescriptor mbeanDesc, String[] attributeNames)
    {
        HashSet<String> expectedAttrNameSet = new HashSet<String>(attributeNames.length);
        HashSet<String> actualAttrNameSet   = new HashSet<String>(attributeNames.length);

        for (String attrName : attributeNames) {
            expectedAttrNameSet.add(attrName.toLowerCase());
        }
        for (String attrName : mbeanDesc.getAttributeNames()) {
            if (expectedAttrNameSet.contains(attrName.toLowerCase())) {
                actualAttrNameSet.add(attrName);
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();

         try {
             AttributeList attrList = mbeanConn.getAttributes(mbeanDesc.getObjectName(), actualAttrNameSet.toArray(new String[actualAttrNameSet.size()]));

             for (Object attrObj : attrList) {
                 Attribute attr = (Attribute)attrObj;

                 result.put(attr.getName(), attr.getValue());
             }
         }
         catch (Exception ex) {
             throw new RuntimeException(ex);
         }
         return result;
    }

    public Map<String, Object> getAttributeValues(MBeanDescriptor mbeanDesc)
    {
        return getAttributeValues(mbeanDesc, mbeanDesc.getAttributeNames());
    }

    public void close() throws IOException
    {
        if (jmxConn != null) {
            jmxConn.close();
            jmxConn = null;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }
}