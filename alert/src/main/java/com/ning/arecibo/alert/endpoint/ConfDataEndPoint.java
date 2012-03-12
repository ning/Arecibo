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

package com.ning.arecibo.alert.endpoint;

import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ConfDataEndPoint<T extends ConfDataObject>
{
    protected final Logger log = LoggerFactory.getLogger(ConfDataEndPoint.class);
    protected final ConfDataDAO dao;

    private final String table;
    private final Class<T> type;

    public ConfDataEndPoint(final ConfDataDAO dao, final String table, final Class<T> type)
    {
        this.dao = dao;
        this.table = table;
        this.type = type;
    }

    protected List<Map<String, Object>> findAllConfDataObject()
    {
        try {
            final List<T> confDataObjectsFound = dao.selectAll(table, type);
            if (confDataObjectsFound == null) {
                throw new WebApplicationException(buildNotFoundResponse());
            }
            else {
                final List<Map<String, Object>> confDataObjects = new ArrayList<Map<String, Object>>();
                for (final T confDataObject : confDataObjectsFound) {
                    confDataObjects.add(confDataObject.toPropertiesMap());
                }
                return confDataObjects;
            }
        }
        catch (ConfDataDAOException e) {
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    protected Map<String, Object> findConfDataObjectById(final Long confDataObjectId)
    {
        try {
            final T confDataObjectFound = dao.selectById(confDataObjectId, table, type);
            if (confDataObjectFound == null) {
                throw new WebApplicationException(buildNotFoundResponse());
            }
            else {
                return confDataObjectFound.toPropertiesMap();
            }
        }
        catch (ConfDataDAOException e) {
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    protected List<Map<String, Object>> findConfDataObjectById(final String columnName, final Object value)
    {
        try {
            final List<T> confDataObjectsFound = dao.selectByColumn(columnName, value, table, type);
            if (confDataObjectsFound == null) {
                throw new WebApplicationException(buildNotFoundResponse());
            }
            else {
                final List<Map<String, Object>> confDataObjects = new ArrayList<Map<String, Object>>();
                for (final T confDataObject : confDataObjectsFound) {
                    confDataObjects.add(confDataObject.toPropertiesMap());
                }
                return confDataObjects;
            }
        }
        catch (ConfDataDAOException e) {
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    // TODO we should we have a whitelist of accepted attributes to avoid users overriding private attributes
    protected Response createConfDataObject(final ConfDataObject confDataObject)
    {
        try {
            final int rowsInserted = dao.insert(confDataObject);
            if (rowsInserted != 1) {
                throw new ConfDataDAOException(String.format("%d rows were inserted!", rowsInserted));
            }

            final List<T> confDataObjectsFound = dao.selectByColumn("label", confDataObject.getLabel(), table, type);
            if (confDataObjectsFound == null || confDataObjectsFound.size() != 1) {
                throw new ConfDataDAOException(String.format("Multiple rows match label=%s!", confDataObject.getLabel()));
            }

            final T confDataObjectCreated = confDataObjectsFound.get(0);
            log.info("Created object: {}", confDataObject);

            final Long confDataObjectId = confDataObjectCreated.getId();
            return Response.created(URI.create(String.format("%d", confDataObjectId))).build();
        }
        catch (ConfDataDAOException e) {
            // TODO fk and unique key violations should return 4xx
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    protected Response deleteConfDataObjectById(final Long confDataObjectId)
    {
        try {
            // TODO handle obvious race condition
            final ConfDataObject confDataObject = dao.selectById(confDataObjectId, table, type);
            if (confDataObject == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            final int rowsDeleted = dao.delete(confDataObject);
            if (rowsDeleted != 1) {
                throw new ConfDataDAOException(String.format("%d rows were deleted!", rowsDeleted));
            }

            log.info("Deleted object: {}", confDataObject);
            return Response.ok().build();
        }
        catch (ConfDataDAOException e) {
            throw new WebApplicationException(e.getCause(), buildServiceUnavailableResponse());
        }
    }

    private Response buildNotFoundResponse()
    {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private Response buildServiceUnavailableResponse()
    {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
    }
}
