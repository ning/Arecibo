package com.ning.arecibo.event.transport;

import com.ning.arecibo.event.MonitoringEvent;
import com.ning.http.client.AsyncHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

public class TestEventServiceRESTClient
{
    @Test(enabled = false)
    public void testSendEvent() throws Exception
    {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        EventServiceRESTClient restClient = new EventServiceRESTClient(httpClient, new JavaEventSerializer(), "client");

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("deployedConfigSubPath", "bleh");
        MonitoringEvent event = new MonitoringEvent(1029304L, "testEvent", map);

        Assert.assertTrue(restClient.sendEvent("127.0.0.1", 8080, event));
    }
}
