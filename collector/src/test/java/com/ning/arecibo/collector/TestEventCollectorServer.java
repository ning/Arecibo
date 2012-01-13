package com.ning.arecibo.collector;

import com.ning.arecibo.collector.guice.CollectorRESTEventReceiverModule;
import com.ning.arecibo.collector.process.CollectorEventProcessor;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.publisher.EventSenderType;
import com.ning.arecibo.event.publisher.RESTEventService;
import com.ning.arecibo.event.receiver.UDPEventReceiverModule;
import com.ning.arecibo.event.transport.EventSerializer;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.JsonEventSerializer;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.EmbeddedJettyJerseyModule;
import com.ning.arecibo.util.lifecycle.LifecycleModule;
import com.ning.arecibo.util.rmi.RMIModule;
import com.ning.arecibo.util.service.DummyServiceLocatorModule;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.http.client.AsyncHttpClient;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

@Guice(modules = {
    LifecycleModule.class,
    DummyServiceLocatorModule.class,
    EmbeddedJettyJerseyModule.class,
    UDPEventReceiverModule.class,
    RMIModule.class,
    CollectorTestModule.class,
    CollectorRESTEventReceiverModule.class
})
public class TestEventCollectorServer
{
    @Inject
    MysqlTestingHelper helper;

    @Inject
    EventCollectorServer server;

    @Inject
    CollectorEventProcessor processor;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception
    {
        final String ddl = IOUtils.toString(TestEventCollectorServer.class.getResourceAsStream("/collector.sql"));

        helper.startMysql();
        helper.initDb(ddl);

        Executors.newFixedThreadPool(1).submit(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    server.run();
                }
                catch (Exception e) {
                    Assert.fail();
                }
            }
        });

        while (!server.isRunning()) {
            Thread.sleep(1000);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception
    {
        helper.stopMysql();
        server.stop();
    }

    @Test(groups = "slow")
    public void testJsonClientIntegration() throws Exception
    {
        final RESTEventService service = createService(new JsonEventSerializer());
        final Event event = createEvent();

        Assert.assertEquals(processor.getEventsReceived(), 0);
        Assert.assertEquals(processor.getEventsDiscarded(), 0);

        for (int i = 1; i < 5; i++) {
            service.sendREST(event);
            Assert.assertEquals(processor.getEventsReceived(), i);
            Assert.assertEquals(processor.getEventsDiscarded(), 0);
        }
    }

    private RESTEventService createService(final EventSerializer serializer)
    {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put(EventService.HOST, "0.0.0.0");
        properties.put(EventService.JETTY_PORT, "8088");
        final ServiceDescriptor localServiceDescriptor = new ServiceDescriptor("testing", properties);

        final AsyncHttpClient client = new AsyncHttpClient();
        final EventServiceRESTClient restClient = new EventServiceRESTClient(client, serializer, EventSenderType.CLIENT);

        return new RESTEventService(new MockEventServiceChooser(), localServiceDescriptor, restClient);
    }

    private Event createEvent()
    {
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("min_heapUsed", Double.valueOf("1.515698888E9"));
        data.put("max_heapUsed", Double.valueOf("1.835511784E9"));

        return new MapEvent(System.currentTimeMillis(), "myType", UUID.randomUUID(), data);
    }
}
