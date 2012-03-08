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
import com.ning.arecibo.alert.confdata.objects.ConfDataAcknowledgementLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataAlertIncidentLog;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.dao.MysqlTestingHelper;
import org.apache.commons.io.IOUtils;
import org.skife.jdbi.v2.DBI;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Guice(modules = AlertDataTestModule.class)
public class TestConfDataDAO
{
    @Inject
    private DBI dbi;

    @Inject
    private MysqlTestingHelper helper;

    private ConfDataQueries dao;
    private ConfDataDAO confDataDAO;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(MysqlTestingHelper.class.getResourceAsStream("/com/ning/arecibo/alert/confdata/create_alert_config_tables.sql"));

        helper.startMysql();
        helper.initDb(ddl);

        dao = dbi.onDemand(ConfDataQueries.class);
        confDataDAO = new ConfDataDAO(dbi);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        helper.stopMysql();
    }

    @Test(groups = "slow")
    public void testInsertAcknowledgementLog() throws Exception
    {
        final List<ConfDataAlertIncidentLog> alertIncidentLogs = confDataDAO.selectAll(ConfDataAlertIncidentLog.TYPE_NAME, ConfDataAlertIncidentLog.class);
        final List<ConfDataPerson> confDataPersons = confDataDAO.selectAll(ConfDataPerson.TYPE_NAME, ConfDataPerson.class);
        final List<ConfDataAcknowledgementLog> acknowledgementLogs = confDataDAO.selectAll(ConfDataAcknowledgementLog.TYPE_NAME, ConfDataAcknowledgementLog.class);
        Assert.assertEquals(alertIncidentLogs.size(), 0);
        Assert.assertEquals(confDataPersons.size(), 0);
        Assert.assertEquals(acknowledgementLogs.size(), 0);

        final long alertIncidentId = createAndCheckAlertIncidentLog();
        final long personId = createAndCheckPerson();
        createAndCheckAcknowledgmentLog(alertIncidentId, personId);
    }

    private long createAndCheckAlertIncidentLog() throws ConfDataDAOException
    {
        final ConfDataAlertIncidentLog alertIncidentLog = new ConfDataAlertIncidentLog();
        alertIncidentLog.setLabel(UUID.randomUUID().toString());
        alertIncidentLog.setContextIdentifier(UUID.randomUUID().toString());
        final long alertIncidentId = dao.insertConfDataAlertIncidentLog(alertIncidentLog);

        // Try select * from ...
        List<ConfDataAlertIncidentLog> alertIncidentLogs = confDataDAO.selectAll(ConfDataAlertIncidentLog.TYPE_NAME, ConfDataAlertIncidentLog.class);
        Assert.assertEquals(alertIncidentLogs.size(), 1);
        basicConfDataChecks(alertIncidentLogs.get(0), alertIncidentId, alertIncidentLog);
        Assert.assertEquals(alertIncidentLogs.get(0).getContextIdentifier(), alertIncidentLog.getContextIdentifier());

        // Try select * from ... where ...
        alertIncidentLogs = confDataDAO.selectByColumn("context_identifier", alertIncidentLog.getContextIdentifier(), ConfDataAlertIncidentLog.TYPE_NAME, ConfDataAlertIncidentLog.class);
        Assert.assertEquals(alertIncidentLogs.size(), 1);
        basicConfDataChecks(alertIncidentLogs.get(0), alertIncidentId, alertIncidentLog);
        Assert.assertEquals(alertIncidentLogs.get(0).getContextIdentifier(), alertIncidentLog.getContextIdentifier());

        // Try select * from ... where id =...
        final ConfDataAlertIncidentLog zeConfDataAlertIncidentLog = confDataDAO.selectById(alertIncidentId, ConfDataAlertIncidentLog.TYPE_NAME, ConfDataAlertIncidentLog.class);
        basicConfDataChecks(zeConfDataAlertIncidentLog, alertIncidentId, alertIncidentLog);
        Assert.assertEquals(zeConfDataAlertIncidentLog.getContextIdentifier(), alertIncidentLog.getContextIdentifier());

        return alertIncidentId;
    }

    private long createAndCheckPerson() throws ConfDataDAOException
    {
        final ConfDataPerson person = new ConfDataPerson();
        person.setLabel(UUID.randomUUID().toString());
        person.setIsGroupAlias(false);
        final long personId = dao.insertConfDataPerson(person);

        // Try select * from ...
        List<ConfDataPerson> confDataPersons = confDataDAO.selectAll(ConfDataPerson.TYPE_NAME, ConfDataPerson.class);
        Assert.assertEquals(confDataPersons.size(), 1);
        basicConfDataChecks(confDataPersons.get(0), personId, person);
        Assert.assertEquals(confDataPersons.get(0).getIsGroupAlias(), person.getIsGroupAlias());

        // Try select * from ... where ...
        confDataPersons = confDataDAO.selectByColumn("is_group_alias", person.getIsGroupAlias(), ConfDataPerson.TYPE_NAME, ConfDataPerson.class);
        Assert.assertEquals(confDataPersons.size(), 1);
        basicConfDataChecks(confDataPersons.get(0), personId, person);
        Assert.assertEquals(confDataPersons.get(0).getIsGroupAlias(), person.getIsGroupAlias());

        // Try select * from ... where id =...
        final ConfDataPerson zeConfDataPerson = confDataDAO.selectById(personId, ConfDataPerson.TYPE_NAME, ConfDataPerson.class);
        basicConfDataChecks(zeConfDataPerson, personId, person);
        Assert.assertEquals(zeConfDataPerson.getIsGroupAlias(), person.getIsGroupAlias());

        return personId;
    }

    private void createAndCheckAcknowledgmentLog(final long alertIncidentId, final long personId) throws ConfDataDAOException
    {
        final ConfDataAcknowledgementLog acknowledgementLog = new ConfDataAcknowledgementLog();
        acknowledgementLog.setLabel(UUID.randomUUID().toString());
        acknowledgementLog.setAlertIncidentId(alertIncidentId);
        acknowledgementLog.setPersonId(personId);
        final long acknowledgmentLogId = dao.insertConfDataAcknowledgementLog(acknowledgementLog);

        // Try select * from ...
        List<ConfDataAcknowledgementLog> acknowledgementLogs = confDataDAO.selectAll(ConfDataAcknowledgementLog.TYPE_NAME, ConfDataAcknowledgementLog.class);
        Assert.assertEquals(acknowledgementLogs.size(), 1);
        basicConfDataChecks(acknowledgementLogs.get(0), acknowledgmentLogId, acknowledgementLog);
        Assert.assertEquals(acknowledgementLogs.get(0).getAlertIncidentId(), acknowledgementLog.getAlertIncidentId());
        Assert.assertEquals(acknowledgementLogs.get(0).getAlertIncidentId(), (Long) alertIncidentId);
        Assert.assertEquals(acknowledgementLogs.get(0).getPersonId(), acknowledgementLog.getPersonId());
        Assert.assertEquals(acknowledgementLogs.get(0).getPersonId(), (Long) personId);

        // Try select * from ... where ...
        acknowledgementLogs = confDataDAO.selectByColumn("label", acknowledgementLog.getLabel(), ConfDataAcknowledgementLog.TYPE_NAME, ConfDataAcknowledgementLog.class);
        Assert.assertEquals(acknowledgementLogs.size(), 1);
        basicConfDataChecks(acknowledgementLogs.get(0), acknowledgmentLogId, acknowledgementLog);
        Assert.assertEquals(acknowledgementLogs.get(0).getAlertIncidentId(), acknowledgementLog.getAlertIncidentId());
        Assert.assertEquals(acknowledgementLogs.get(0).getAlertIncidentId(), (Long) alertIncidentId);
        Assert.assertEquals(acknowledgementLogs.get(0).getPersonId(), acknowledgementLog.getPersonId());
        Assert.assertEquals(acknowledgementLogs.get(0).getPersonId(), (Long) personId);

        // Try select * from ... where id =...
        final ConfDataAcknowledgementLog zeConfDataAcknowledgmentLog = confDataDAO.selectById(acknowledgmentLogId, ConfDataAcknowledgementLog.TYPE_NAME, ConfDataAcknowledgementLog.class);
        basicConfDataChecks(zeConfDataAcknowledgmentLog, acknowledgmentLogId, acknowledgementLog);
        Assert.assertEquals(zeConfDataAcknowledgmentLog.getAlertIncidentId(), acknowledgementLog.getAlertIncidentId());
        Assert.assertEquals(zeConfDataAcknowledgmentLog.getAlertIncidentId(), (Long) alertIncidentId);
        Assert.assertEquals(zeConfDataAcknowledgmentLog.getPersonId(), acknowledgementLog.getPersonId());
        Assert.assertEquals(zeConfDataAcknowledgmentLog.getPersonId(), (Long) personId);
    }

    private void basicConfDataChecks(final ConfDataObject actual, final long id, final ConfDataObject expected)
    {
        final Timestamp now = new Timestamp(System.currentTimeMillis());

        Assert.assertEquals(actual.getId(), (Long) id);
        Assert.assertEquals(actual.getLabel(), expected.getLabel());
        Assert.assertTrue(actual.getCreateTimestamp().before(now));
        Assert.assertTrue(actual.getUpdateTimestamp().before(now));
    }
}
