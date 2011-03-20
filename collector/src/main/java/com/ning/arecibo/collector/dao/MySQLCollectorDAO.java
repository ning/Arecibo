package com.ning.arecibo.collector.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.collector.config.CollectorConfig;
import com.ning.arecibo.collector.guice.CollectorModule;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerMapper;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLCollectorDAO extends CollectorDAO
{
    @Inject
    public MySQLCollectorDAO(@Named(CollectorModule.COLLECTOR_DB) IDBI dbi,
                             CollectorConfig config,
                             Registry registry,
                             ResolutionUtils resolutionUtils)
        throws RemoteException, AlreadyBoundException
    {
        super(dbi, config, registry, resolutionUtils, "com/ning/arecibo/collector/dao/mysql");
    }

    @Override
    protected CachedHost getCachedHost(final CachedHost cachedHost, final String hostName) throws CollectorDAOException
    {
        try {
            if (!hostMap.containsKey(hostName)) {
                CachedHost saved;
                saved = dbi.withHandle(new HandleCallback<CachedHost>()
                {
                    public CachedHost withHandle(Handle handle) throws Exception
                    {
                        return handle.createQuery(statementsLocation + ":getHostByName")
                            .bind("host", hostName)
                            .map(new ResultSetMapper<CachedHost>()
                            {
                                public CachedHost map(int i, ResultSet rs, StatementContext statementContext) throws SQLException
                                {
                                    return new CachedHost(rs);
                                }
                            }).first();
                    }
                });

                if (saved == null) {
                    CachedHost old = hostMap.putIfAbsent(hostName, cachedHost);
                    if (old == null) {
                        if (!readOnlyMode) {
                            try {
                                dbi.withHandle(new HandleCallback<Object>()
                                {
                                    public Object withHandle(Handle handle) throws Exception
                                    {
                                        handle.createStatement(statementsLocation + ":insertHost")
                                            .bind("host", cachedHost.getHost())
                                            .bind("dep_type", cachedHost.getType())
                                            .bind("dep_path", cachedHost.getPath())
                                            .execute();
                                        return null;
                                    }
                                });

                                // Return new host
                                return dbi.withHandle(new HandleCallback<CachedHost>()
                                {
                                    public CachedHost withHandle(Handle handle) throws Exception
                                    {
                                        return handle.createQuery(statementsLocation + ":getHostByName")
                                            .bind("host", hostName)
                                            .map(new ResultSetMapper<CachedHost>()
                                            {
                                                public CachedHost map(int i, ResultSet rs, StatementContext statementContext) throws SQLException
                                                {
                                                    return new CachedHost(rs);
                                                }
                                            }).first();
                                    }
                                });
                            }
                            catch (RuntimeException ruEx) {
                                throw new CollectorDAOException("RuntimeException:", ruEx);
                            }
                        }

                        return cachedHost;
                    }
                    else {
                        return old;
                    }
                }
                else {
                    hostMap.putIfAbsent(hostName, saved);
                    return saved;
                }
            }
            else {
                CachedHost cached = hostMap.get(hostName);
                cached.update(cachedHost);
                return cached;
            }
        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException:", ruEx);
        }
    }

    @Override
    protected Integer getCachedTypeID(final String type) throws CollectorDAOException
    {
        try {

            if (!typeMap.containsKey(type)) {
                Integer saved = null;
                saved = dbi.withHandle(new HandleCallback<Integer>()
                {
                    public Integer withHandle(Handle handle) throws Exception
                    {
                        return handle.createQuery(statementsLocation + ":getTypeByName")
                            .bind("dep_type", type)
                            .map(IntegerMapper.FIRST).first();
                    }
                });

                if (saved == null) {
                    if (!readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(statementsLocation + ":insertType")
                                    .bind("dep_type", type)
                                    .execute();
                                return null;
                            }
                        });

                        // Retrieve the ID
                        saved = dbi.withHandle(new HandleCallback<Integer>()
                        {
                            public Integer withHandle(Handle handle) throws Exception
                            {
                                return handle.createQuery(statementsLocation + ":getTypeByName")
                                    .bind("dep_type", type)
                                    .map(IntegerMapper.FIRST).first();
                            }
                        });
                        typeMap.putIfAbsent(type, saved);
                    }
                }
                else {
                    typeMap.putIfAbsent(type, saved);
                }
            }
            return typeMap.get(type);

        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException:", ruEx);
        }
    }

    @Override
    protected Integer getCachedPathID(final String path) throws CollectorDAOException
    {
        try {

            if (!pathMap.containsKey(path)) {
                Integer saved;
                saved = dbi.withHandle(new HandleCallback<Integer>()
                {
                    public Integer withHandle(Handle handle) throws Exception
                    {
                        return handle.createQuery(statementsLocation + ":getPathByName")
                            .bind("dep_path", path)
                            .map(IntegerMapper.FIRST).first();
                    }
                });

                if (saved == null) {
                    if (!readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(statementsLocation + ":insertPath")
                                    .bind("dep_path", path)
                                    .execute();
                                return null;
                            }
                        });
                    }

                    // Retrieve the ID
                    saved = dbi.withHandle(new HandleCallback<Integer>()
                    {
                        public Integer withHandle(Handle handle) throws Exception
                        {
                            return handle.createQuery(statementsLocation + ":getPathByName")
                                .bind("dep_path", path)
                                .map(IntegerMapper.FIRST).first();
                        }
                    });
                    pathMap.putIfAbsent(path, saved);
                }
                else {
                    pathMap.putIfAbsent(path, saved);
                }
            }
            return pathMap.get(path);

        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException:", ruEx);
        }
    }


    @Override
    protected Integer getCachedEventTypeID(final String eventType) throws CollectorDAOException
    {
        try {
            if (!eventTypeMap.containsKey(eventType)) {
                Integer saved;
                saved = dbi.withHandle(new HandleCallback<Integer>()
                {
                    public Integer withHandle(Handle handle) throws Exception
                    {
                        return handle.createQuery(statementsLocation + ":getEventTypeByName")
                            .bind("event_type", eventType)
                            .map(IntegerMapper.FIRST).first();
                    }
                });

                if (saved == null) {
                    if (!readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(statementsLocation + ":insertEventType")
                                    .bind("event_type", eventType)
                                    .execute();
                                return null;
                            }
                        });

                        // Retrieve the ID
                        saved = dbi.withHandle(new HandleCallback<Integer>()
                        {
                            public Integer withHandle(Handle handle) throws Exception
                            {
                                return handle.createQuery(statementsLocation + ":getEventTypeByName")
                                    .bind("event_type", eventType)
                                    .map(IntegerMapper.FIRST).first();
                            }
                        });
                        eventTypeMap.putIfAbsent(eventType, saved);
                    }
                }
                else {
                    eventTypeMap.putIfAbsent(eventType, saved);
                }
            }
            return eventTypeMap.get(eventType);
        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException:", ruEx);
        }
    }
}
