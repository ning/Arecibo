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
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.LongMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConfDataDAO
{
    static final Logger log = Logger.getLogger(ConfDataDAO.class);

    private final IDBI dbi;

    @Inject
    public ConfDataDAO(final IDBI dbi)
    {
        this.dbi = dbi;
    }

    public <T extends ConfDataObject> Long insert(final T data)
        throws ConfDataDAOException
    {

        try {

            final Long insertId = dbi.withHandle(new HandleCallback<Long>()
            {
                public Long withHandle(final Handle handle) throws Exception
                {
                    return handle.inTransaction(new TransactionCallback<Long>()
                    {
                        public Long inTransaction(final Handle handle, final TransactionStatus transactionStatus) throws Exception
                        {
                            return insert(handle, data);
                        }
                    });
                }
            });

            return insertId;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting data:", e);
        }
    }

    public <T extends ConfDataObject> Long update(final T data)
        throws ConfDataDAOException
    {

        try {

            final Long updateCount = dbi.withHandle(new HandleCallback<Long>()
            {
                public Long withHandle(final Handle handle) throws Exception
                {
                    return handle.inTransaction(new TransactionCallback<Long>()
                    {
                        public Long inTransaction(final Handle handle, final TransactionStatus transactionStatus) throws Exception
                        {
                            return update(handle, data);
                        }
                    });
                }
            });

            return updateCount;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem updating data:", e);
        }
    }

    public Long delete(final Long id,
                       final String table)
        throws ConfDataDAOException
    {
        try {
            final Long deleteCount = dbi.withHandle(new HandleCallback<Long>()
            {
                public Long withHandle(final Handle handle) throws Exception
                {
                    return handle.inTransaction(new TransactionCallback<Long>()
                    {
                        public Long inTransaction(final Handle handle, final TransactionStatus transactionStatus) throws Exception
                        {
                            return delete(handle, id, table);
                        }
                    });
                }
            });

            return deleteCount;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem deleting data in table " + table, e);
        }
    }

    public <T extends ConfDataObject> List<T> selectAll(final String table,
                                                        final Class<T> type)
        throws ConfDataDAOException
    {

        final ResultSetMapper<T> mapper = new _ObjectTypeMapper<T>(type);
        return selectAll(table, mapper);
    }

    public <T> List<T> selectAll(final String table,
                                 final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_all")
                        .define("table", table)
                        .map(mapper).list();
                }
            });
            return rows;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data from table " + table, e);
        }
    }

    public <T extends ConfDataObject> T selectById(final Long id,
                                                   final String table,
                                                   final Class<T> type)
        throws ConfDataDAOException
    {

        final ResultSetMapper<T> mapper = new _ObjectTypeMapper<T>(type);
        return selectById(id, table, mapper);
    }


    private <T extends ConfDataObject> T selectById(final Long id,
                                                    final String table,
                                                    final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_by_id")
                        .define("table", table)
                        .bind("id", id)
                        .map(mapper).list();
                }
            });

            if (rows != null && rows.size() > 0) {
                return rows.get(0);
            }
            else {
                return null;
            }
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }

    public <T extends ConfDataObject> List<T> selectByColumn(final String column,
                                                             final Object columnVal,
                                                             final String table,
                                                             final Class<T> type)
        throws ConfDataDAOException
    {

        final ResultSetMapper<T> mapper = new _ObjectTypeMapper<T>(type);

        if (columnVal == null) {
            return selectByColumnIsNull(column, table, mapper);
        }
        else {
            return selectByColumn(column, columnVal, table, mapper);
        }
    }


    private <T extends ConfDataObject> List<T> selectByColumn(final String column,
                                                              final Object columnVal,
                                                              final String table,
                                                              final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_by_column")
                        .define("table", table)
                        .define("column", column)
                        .bind("columnVal", columnVal)
                        .map(mapper).list();
                }
            });

            return rows;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }

    private <T extends ConfDataObject> List<T> selectByColumnIsNull(final String column,
                                                                    final String table,
                                                                    final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_by_column_is_null")
                        .define("table", table)
                        .define("column", column)
                        .map(mapper).list();
                }
            });

            return rows;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }

    public <T extends ConfDataObject> List<T> selectByDateColumnRange(final String column,
                                                                      final long columnVal1,
                                                                      final long columnVal2,
                                                                      final String table,
                                                                      final Class<T> type)
        throws ConfDataDAOException
    {

        final ResultSetMapper<T> mapper = new _ObjectTypeMapper<T>(type);
        return selectByDateColumnRange(column, columnVal1, columnVal2, table, mapper);
    }


    private <T extends ConfDataObject> List<T> selectByDateColumnRange(final String column,
                                                                       final long columnVal1,
                                                                       final long columnVal2,
                                                                       final String table,
                                                                       final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_by_column_range")
                        .define("table", table)
                        .define("column", column)
                        .bind("columnVal1", new Timestamp(columnVal1))
                        .bind("columnVal2", new Timestamp(columnVal2))
                        .map(mapper).list();
                }
            });

            return rows;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data table " + table, e);
        }
    }


    public <T extends ConfDataObject> List<T> selectBySecondaryDoubleColumnRange(final String column1,
                                                                                 final String table1,
                                                                                 final String column2,
                                                                                 final String column2a,
                                                                                 final String column2b,
                                                                                 final String table2,
                                                                                 final long columnVal2a1,
                                                                                 final long columnVal2a2,
                                                                                 final long columnVal2b1,
                                                                                 final long columnVal2b2,
                                                                                 final Class<T> type)
        throws ConfDataDAOException
    {

        final ResultSetMapper<T> mapper = new _ObjectTypeMapper<T>(type);
        return selectBySecondaryDoubleColumnRange(column1, table1, column2, column2a, column2b, table2, columnVal2a1, columnVal2a2, columnVal2b1, columnVal2b2, mapper);
    }

    public <T extends ConfDataObject> List<T> selectBySecondaryDoubleColumnRange(final String column1,
                                                                                 final String table1,
                                                                                 final String column2,
                                                                                 final String column2a,
                                                                                 final String column2b,
                                                                                 final String table2,
                                                                                 final long columnVal2a1,
                                                                                 final long columnVal2a2,
                                                                                 final long columnVal2b1,
                                                                                 final long columnVal2b2,
                                                                                 final ResultSetMapper<T> mapper)
        throws ConfDataDAOException
    {

        try {
            final List<T> rows = dbi.withHandle(new HandleCallback<List<T>>()
            {
                public List<T> withHandle(final Handle handle) throws Exception
                {
                    return handle.createQuery(getClass().getPackage().getName() + ":select_by_secondary_double_column_range")
                        .define("table1", table1)
                        .define("column1", column1)
                        .define("table2", table2)
                        .define("column2", column2)
                        .define("column2a", column2a)
                        .define("column2b", column2b)
                        .bind("columnVal2a1", new Timestamp(columnVal2a1))
                        .bind("columnVal2a2", new Timestamp(columnVal2a2))
                        .bind("columnVal2b1", new Timestamp(columnVal2b1))
                        .bind("columnVal2b2", new Timestamp(columnVal2b2))
                        .map(mapper).list();
                }
            });

            return rows;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem selecting data tables " + table1 + " and/or " + table2, e);
        }
    }


    public <T extends ConfDataObject> Long delete(final T data)
        throws ConfDataDAOException
    {

        return delete(data.getId(), data.getTypeName());
    }


    public Long compoundInsertUpdateDelete(final Iterable<ConfDataObject> insertIter,
                                           final Iterable<ConfDataObject> updateIter,
                                           final Iterable<ConfDataObject> deleteIter)
        throws ConfDataDAOException
    {
        try {
            final Long changeCount = dbi.withHandle(new HandleCallback<Long>()
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
                                    insert(handle, insertObj);
                                    count++;
                                }
                            }

                            if (updateIter != null) {
                                for (final ConfDataObject updateObj : updateIter) {
                                    update(handle, updateObj);
                                    count++;
                                }
                            }

                            if (deleteIter != null) {
                                for (final ConfDataObject deleteObj : deleteIter) {
                                    delete(handle, deleteObj);
                                    count++;
                                }
                            }

                            return count;
                        }
                    });
                }

            });

            return changeCount;
        }
        catch (RuntimeException e) {
            log.warn(e);
            throw new ConfDataDAOException("Problem inserting, updating and/or deleting data", e);
        }
    }

    // private methods
    private <T extends ConfDataObject> Long insert(final Handle handle, final T data)
    {


        final long sequence = handle.createQuery(getClass().getPackage().getName() + ":nextId")
            .map(new LongMapper())
            .first();

        // update this id in the data object
        data.setId(sequence);

        final String templateName = data.getInsertSqlTemplateName();
        final Map<String, Object> bindings = data.toPropertiesMap();

        final Update update = handle.createStatement(ConfDataObject.class.getPackage().getName() + templateName);

        // add in bindings
        for (final Map.Entry<String, Object> entry : bindings.entrySet()) {

            log.debug("Binding insert values: %s = %s", entry.getKey(), entry.getValue());

            if (entry.getValue() == null) {
                update.bindNull(entry.getKey(), Types.NULL);
            }
            else {
                update.bind(entry.getKey(), entry.getValue());
            }
        }

        final int count = update.execute();
        if (count != 1) {
            throw new RuntimeException("Unable to insert using sql template '" + templateName + "'");
        }

        return sequence;
    }

    private <T extends ConfDataObject> Long update(final Handle handle, final T data)
    {

        final String templateName = data.getUpdateSqlTemplateName();
        final Map<String, Object> bindings = data.toPropertiesMap();

        final Update update = handle.createStatement(ConfDataObject.class.getPackage().getName() + templateName)
            .bind("id", data.getId());

        // add in bindings
        for (final Map.Entry<String, Object> entry : bindings.entrySet()) {

            log.debug("Binding update values: %s = %s", entry.getKey(), entry.getValue());

            if (entry.getValue() == null) {
                update.bindNull(entry.getKey(), Types.NULL);
            }
            else {
                update.bind(entry.getKey(), entry.getValue());
            }
        }

        final int count = update.execute();
        if (count != 1) {
            throw new RuntimeException("Unable to update data using sql template '" + templateName + "'");
        }

        return (long) count;
    }

    private <T extends ConfDataObject> Long delete(final Handle handle, final T data)
    {
        return delete(handle, data.getId(), data.getTypeName());
    }

    private Long delete(final Handle handle, final Long id, final String table)
    {

        final Update update = handle.createStatement(getClass().getPackage().getName() + ":delete_by_id")
            .define("table", table)
            .bind("id", id);

        final int count = update.execute();
        if (count < 1) {
            throw new RuntimeException("Unable to delete from " + table);
        }

        return (long) count;
    }

    public class _ObjectTypeMapper<T extends ConfDataObject> implements ResultSetMapper<T>
    {
        private final Class<T> type;

        public _ObjectTypeMapper(final Class<T> type)
        {
            this.type = type;
        }

        public T map(final int index, final ResultSet r, final StatementContext ctx) throws SQLException
        {
            final Map<String, Object> resultMap = new HashMap<String, Object>();

            final ResultSetMetaData rsmd = r.getMetaData();
            final int numColumns = rsmd.getColumnCount();

            for (int i = 1; i <= numColumns; i++) {
                final int columnType = rsmd.getColumnType(i);
                final String columnName = rsmd.getColumnName(i).toLowerCase();

                final Object obj;
                if (columnType == Types.TIMESTAMP) {
                    // need to do this, to convert from Oracle's timestamp class
                    obj = r.getTimestamp(columnName);
                }
                else {
                    obj = r.getObject(columnName);
                }

                resultMap.put(columnName, obj);
            }

            try {
                final T obj = type.newInstance();
                obj.populatePropertiesFromMap(resultMap);
                return obj;
            }
            catch (InstantiationException instEx) {
                throw new IllegalArgumentException(instEx);
            }
            catch (IllegalAccessException illAccEx) {
                throw new IllegalArgumentException(illAccEx);
            }
        }
    }
}
