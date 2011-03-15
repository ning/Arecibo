package com.ning.arecibo.alert.logging;

import com.google.inject.Inject;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;
import com.ning.arecibo.alert.confdata.dao.ConfDataDAOException;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertIncidentLog;
import com.ning.arecibo.alert.objects.AlertIncidentLog;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.DateTimeZone;

import com.ning.arecibo.util.Logger;



//TODO: perhaps rethink exception handling here....

public class LoggingManager {
    final static Logger log = Logger.getLogger(LoggingManager.class);

    final static DateTimeFormatter basicDateTimeFormatter = ISODateTimeFormat.basicDateTimeNoMillis().withZone(DateTimeZone.forOffsetHours(0));

    private final ConfDataDAO confDataDAO;

    @Inject
    public LoggingManager(ConfDataDAO confDataDAO) {
        this.confDataDAO = confDataDAO;
    }

    public AlertIncidentLog insertAlertIncidentLogEntry(AlertIncidentLog incident) {
        try {

            incident.setLabel(incident.getContextIdentifier() + " " + basicDateTimeFormatter.print(incident.getStartTime().getTime()));

            Long id = this.confDataDAO.insert(incident);
            incident.setId(id);
            return incident;
        }
        catch(ConfDataDAOException cddEx) {
            log.warn(cddEx);
            return null;
        }
    }

    public boolean updateAlertIncidentLogEntry(AlertIncidentLog incident) {
        try {
            this.confDataDAO.update(incident);
            return true;
        }
        catch(ConfDataDAOException cddEx) {
            log.warn(cddEx);
            return false;
        }
    }

    public AlertIncidentLog selectAlertIncidentLogEntry(Long incidentId) {
        try {
            AlertIncidentLog aiLog = (AlertIncidentLog)this.confDataDAO.selectById(incidentId,ConfDataAlertIncidentLog.TYPE_NAME,ConfDataAlertIncidentLog.class);
            return aiLog;
        }
        catch(ConfDataDAOException cddEx) {
            log.warn(cddEx);
            return null;
        }
    }
}
