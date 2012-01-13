package com.ning.arecibo.collector;

import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ning.arecibo.collector.guice.CollectorConstants;
import com.ning.arecibo.collector.guice.CollectorModule;
import com.ning.arecibo.util.timeline.TimelineDAO;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;

public class CollectorTestModule extends CollectorModule
{
    @Override
    protected void configureDao()
    {
        final MysqlTestingHelper helper = new MysqlTestingHelper();
        bind(MysqlTestingHelper.class).toInstance(helper);
        final DBI dbi = helper.getDBI();

        final Named moduleName = Names.named(CollectorConstants.COLLECTOR_DB);
        bind(DBI.class).annotatedWith(moduleName).toInstance(dbi);
        bind(IDBI.class).annotatedWith(moduleName).toInstance(dbi);
        bind(TimelineDAO.class).asEagerSingleton();
    }
}
