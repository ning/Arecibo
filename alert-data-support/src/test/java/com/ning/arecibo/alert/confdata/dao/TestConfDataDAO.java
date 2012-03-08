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
import com.ning.arecibo.alert.confdata.objects.ConfDataPerson;
import com.ning.arecibo.dao.MysqlTestingHelper;
import org.apache.commons.io.IOUtils;
import org.skife.jdbi.v2.DBI;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.UUID;

@Guice(modules = AlertDataTestModule.class)
public class TestConfDataDAO
{
    @Inject
    private DBI dbi;

    @Inject
    private MysqlTestingHelper helper;

    private NewDAO dao;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(MysqlTestingHelper.class.getResourceAsStream("/com/ning/arecibo/alert/confdata/create_alert_config_tables.sql"));

        helper.startMysql();
        helper.initDb(ddl);

        dao = dbi.onDemand(NewDAO.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        helper.stopMysql();
    }

    @Test(groups = "slow")
    public void testInsert() throws Exception
    {
        final ConfDataAlertIncidentLog alertIncidentLog = new ConfDataAlertIncidentLog();
        alertIncidentLog.setLabel(UUID.randomUUID().toString());
        alertIncidentLog.setContextIdentifier(UUID.randomUUID().toString());
        final long alertIncidentId = dao.insertAlertIncidentLog(alertIncidentLog);

        final ConfDataPerson person = new ConfDataPerson();
        person.setLabel(UUID.randomUUID().toString());
        person.setIsGroupAlias(false);
        final long personId = dao.insertPerson(person);

        final ConfDataAcknowledgementLog acknowledgementLog = new ConfDataAcknowledgementLog();
        acknowledgementLog.setLabel(UUID.randomUUID().toString());
        acknowledgementLog.setAlertIncidentId(alertIncidentId);
        acknowledgementLog.setPersonId(personId);
        dao.insertAcknowledgementLog(acknowledgementLog);
    }
}
