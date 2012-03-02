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

package com.ning.arecibo.dashboard.alert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.ning.arecibo.alert.client.AlertStatus;
import com.ning.arecibo.alert.client.AlertStatusJSONConverter;
import com.ning.arecibo.util.OutputStreamAsyncHandler;
import com.ning.http.client.AsyncHttpClient;

public class AlertRESTClient
{
    //TODO: This should be injected?
	private final static String API_PATH = "/xn/rest/1.0/JSONAlertStatus";

	private final AsyncHttpClient httpClient;

	public AlertRESTClient()
	{
		this.httpClient = new AsyncHttpClient();
	}

	public List<DashboardAlertStatus> getAlertStatus(String host, int port, final long generationCount) throws IOException
	{
	    String url = String.format("http://%s:%d%s", host, port, API_PATH);
	    OutputStreamAsyncHandler handler = new OutputStreamAsyncHandler();

        httpClient.preparePost(url).setBody(new byte[0]).execute(handler);

        List<AlertStatus> baseList = AlertStatusJSONConverter.serializeJSONToStatusList(handler.getInputStream());
        if (baseList == null) {
            return null;
        }

        List<DashboardAlertStatus> retList = new ArrayList<DashboardAlertStatus>();
        for (AlertStatus baseStatus:baseList) {
            retList.add(new DashboardAlertStatus(baseStatus,generationCount));
        }

        return retList;
	}
}
