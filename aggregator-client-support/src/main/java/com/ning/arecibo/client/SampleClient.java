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

package com.ning.arecibo.client;

import java.rmi.RemoteException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.lang.AggregatorCallback;
import com.ning.arecibo.lang.ConstantDispatchRouter;
import com.ning.arecibo.lang.DispatcherCallback;
import com.ning.arecibo.lang.ExternalPublisher;
import com.ning.arecibo.lang.InternalDispatcher;

import com.ning.arecibo.util.service.ServiceNotAvailableException;

public class SampleClient
{
	public static void main(String args[]) throws ServiceNotAvailableException, RemoteException
	{

		Aggregator agg = new Aggregator("dynamic", "ContentDAO_QC", "ContentDAO")
			.setStatement(" select max(QueryCount) as QueryCount, count(*) as datapoints, hostName" +
					" from ContentDAO(eventType = 'ContentDAO').win:time(1 min)" +
					" group by hostName"
			)
			.setOutputEvent("ContentDAO_QC")
			.addOutputProcessor(new ExternalPublisher("AlertCore"))
			.addDispatcher(new ConstantDispatchRouter("ContentDAO_QC"), new DispatcherCallback()
			{
				public void configure(InternalDispatcher dispatcher)
				{
					dispatcher
					.addAggregator("SUM", new AggregatorCallback()
                    {
						public void configure(Aggregator agg)
						{
							agg.setStatement(
								"select sum(QueryCount) as sumQueryCount, sum(datapoints) as datapoints, count(*) as countstar" +
								" from ContentDAO_QC ( eventType = 'ContentDAO_QC' and datapoints is not null ).std:unique(hostName) "
							)
							.setOutputEvent("ContentDAO_QC_SUM2");
						}
					})
					;
				}
			});

        System.out.println(agg.getInputEvent());

        Injector injector = Guice.createInjector(Stage.PRODUCTION,
                // TODO: need to bind an implementation of ServiceLocator
		        new AggregatorClientModule());

		AggregatorService service = injector.getInstance(AggregatorService.class);

		service.getAggregatorService().register(agg);
		//service.getAggregatorService().unregister(agg.getFullName());
		System.exit(0);
	 }
}
