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

package com.ning.arecibo.alert.confdata.dao;

import com.google.inject.Inject;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.util.Logger;
import org.skife.jdbi.v2.BeanMapper;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;

public class ConfDataDAO
{
    static final Logger log = Logger.getLogger(ConfDataDAO.class);

    private final DBI dbi;
    private final ConfDataQueries dao;

    @Inject
    public ConfDataDAO(final DBI dbi)
    {
        this.dbi = dbi;
        this.dao = dbi.onDemand(ConfDataQueries.class);
    }

    public <T extends ConfDataObject> Integer insert(final T data) throws ConfDataDAOException
    {
        return performDAOOperation("insert", data);
    }

    public <T extends ConfDataObject> Integer update(final T data) throws ConfDataDAOException
    {
        return performDAOOperation("update", data);
    }

    public <T extends ConfDataObject> Integer delete(final T data) throws ConfDataDAOException
    {
        return performDAOOperation("delete", data);
    }

    public <T extends ConfDataObject> List<T> selectAll(final String table, final Class<T> type) throws ConfDataDAOException
    {
        try {
            return dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery("select * from " + table)
                        .map(new LowerToCamelBeanMapper<T>(type))
                        .list();
                }
            });
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data from table " + table, e);
        }
    }

    public <T extends ConfDataObject> T selectById(final Long id, final String table, final Class<T> type) throws ConfDataDAOException
    {
        final List<T> candidates = selectByColumn("id", id, table, type);
        if (candidates == null || candidates.size() == 0) {
            return null;
        }
        else {
            return candidates.get(0);
        }
    }

    public <T extends ConfDataObject> List<T> selectByColumn(final String column, final Object columnVal, final String table, final Class<T> type) throws ConfDataDAOException
    {
        try {
            if (columnVal == null) {
                return dbi.withHandle(new HandleCallback<List<T>>()
                {
                    public List<T> withHandle(final Handle handle) throws Exception
                    {
                        return handle.createQuery("select * from " + table + " where " + columnVal + " is null")
                            .map(new LowerToCamelBeanMapper<T>(type))
                            .list();
                    }
                });
            }
            else {
                return dbi.withHandle(new HandleCallback<List<T>>()
                {
                    public List<T> withHandle(final Handle handle) throws Exception
                    {
                        return handle.createQuery("select * from " + table + " where " + column + " = :columnVal")
                            .bind("columnVal", columnVal)
                            .map(new LowerToCamelBeanMapper<T>(type))
                            .list();
                    }
                });
            }
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }

    public <T extends ConfDataObject> List<T> selectByDateColumnRange(final String column, final long columnVal1, final long columnVal2, final String table, final Class<T> type) throws ConfDataDAOException
    {
        try {
            return dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery("select * from " + table + " where " + column + " between :columnVal1 and :columnVal2")
                        .bind("columnVal1", new Timestamp(columnVal1))
                        .bind("columnVal2", new Timestamp(columnVal2))
                        .map(new BeanMapper<T>(type))
                        .list();
                }
            });
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }

    private <T extends ConfDataObject> Integer performDAOOperation(final String bashMethodName, final T data) throws ConfDataDAOException
    {
        try {
            final String[] classTokenized = data.getClass().getName().split("\\.");
            final Method method = dao.getClass().getMethod(bashMethodName + classTokenized[classTokenized.length - 1], data.getClass());
            return (Integer) method.invoke(dao, data);
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting data:", e);
        }
        catch (NoSuchMethodException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting data:", e);
        }
        catch (InvocationTargetException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting data:", e);
        }
        catch (IllegalAccessException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting data:", e);
        }
    }

    public Long compoundInsertUpdateDelete(final Iterable<ConfDataObject> insertIter,
                                           final Iterable<ConfDataObject> updateIter,
                                           final Iterable<ConfDataObject> deleteIter)
        throws ConfDataDAOException
    {
        try {
            return dbi.withHandle(new HandleCallback<Long>()
            {
                public Long withHandle(final Handle handle) throws Exception
                {
                    return handle.inTransaction(new TransactionCallback<Long>()
                    {
                        public Long inTransaction(final Handle handle, final TransactionStatus transactionStatus) throws Exception
                        {
                            Long count = 0L;

                            if (insertIter != null) {
                                for (final ConfDataObject insertObj : insertIter) {
                                    insert(insertObj);
                                    count++;
                                }
                            }

                            if (updateIter != null) {
                                for (final ConfDataObject updateObj : updateIter) {
                                    update(updateObj);
                                    count++;
                                }
                            }

                            if (deleteIter != null) {
                                for (final ConfDataObject deleteObj : deleteIter) {
                                    delete(deleteObj);
                                    count++;
                                }
                            }

                            return count;
                        }
                    });
                }
            });
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting, updating and/or deleting data", e);
        }
    }
}
