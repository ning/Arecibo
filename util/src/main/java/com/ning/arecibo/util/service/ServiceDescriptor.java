package com.ning.arecibo.util.service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class ServiceDescriptor
{
    public static final String DEFAULT_POOL_NAME = "general";

    private final String name;
    private final String pool;
    private final Map<String, String> properties;
    private final UUID uuid;

    /**
     *
     * @param name Service name
     * @param pool A name to help split up pools of services
     * @param properties Arbitrary properties for the service
     */
    public ServiceDescriptor(String name, String pool, Map<String, String> properties)
    {
        this.name = name;
        this.pool = pool;
        this.properties = properties;
        this.uuid = UUID.randomUUID();
    }

    /**
     *
     * @param name Service name
     * @param pool A name to help split up pools of services
     * @param properties Arbitrary properties for the service
     * @param uuid Unique identifier for the service
     */
    public ServiceDescriptor(UUID uuid, String name, String pool, Map<String, String> properties)
    {
        this.name = name;
        this.pool = pool;
        this.properties = properties;
        this.uuid = uuid;
    }


    public ServiceDescriptor(UUID uuid, String name, Map<String, String> properties)
    {
        this(uuid, name, DEFAULT_POOL_NAME, properties);
    }


    public ServiceDescriptor(String name, Map<String, String> properties)
    {
        this(name, DEFAULT_POOL_NAME, properties);
    }

    public ServiceDescriptor(String name)
    {
        this(name, Collections.<String, String>emptyMap());
    }

    public String getName()
    {
        return name;
    }

    public Map<String, String> getProperties()
    {
        return Collections.unmodifiableMap(properties);
    }

    public String getProperty(final String key)
    {
        return properties.get(key);
    }

    public UUID getUuid()
    {
        return uuid;
    }

    @Override
    public String toString()
    {
        return "ServiceDescriptor [name=" + name + ", pool=" + pool + ", properties=" + properties + ", uuid=" + uuid + "]";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceDescriptor that = (ServiceDescriptor) o;

        if (!name.equals(that.name)) return false;
        if (!pool.equals(that.pool)) return false;
        if (!uuid.equals(that.uuid)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result;
        result = name.hashCode();
        result = 31 * result + pool.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    public String getPool()
    {
        return pool;
    }
}
