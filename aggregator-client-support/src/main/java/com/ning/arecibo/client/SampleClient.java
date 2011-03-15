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
