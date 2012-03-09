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

package com.ning.arecibo.alert.logging;

import com.google.inject.Inject;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.objects.AlertIncidentLog;
import com.ning.arecibo.util.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

//TODO: perhaps rethink exception handling here....
public class LoggingManager
{
    static final Logger log = Logger.getLogger(LoggingManager.class);

    static final DateTimeFormatter basicDateTimeFormatter = ISODateTimeFormat.basicDateTimeNoMillis().withZone(DateTimeZone.forOffsetHours(0));

    private final ConfDataDAO confDataDAO;

    @Inject
    public LoggingManager(final ConfDataDAO confDataDAO)
    {
        this.confDataDAO = confDataDAO;
    }

    public AlertIncidentLog insertAlertIncidentLogEntry(final AlertIncidentLog incident)
    {
        try {
            incident.setLabel(incident.getContextIdentifier() + " " + basicDateTimeFormatter.print(incident.getStartTime().getTime()));

            final Integer id = this.confDataDAO.insert(incident);
            incident.setId(id.longValue());
            return incident;
        }
        catch (ConfDataDAOException cddEx) {
            log.warn(cddEx);
            return null;
        }
    }

    public boolean updateAlertIncidentLogEntry(final AlertIncidentLog incident)
    {
        try {
            this.confDataDAO.update(incident);
            return true;
        }
        catch (ConfDataDAOException cddEx) {
            log.warn(cddEx);
            return false;
        }
    }

}
