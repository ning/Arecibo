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

package com.ning.arecibo.aggregator.impl;

import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.dictionary.EventDefinition;
import com.ning.arecibo.aggregator.dictionary.EventDictionary;
import com.ning.arecibo.aggregator.esper.EsperProvider;
import com.ning.arecibo.aggregator.esper.EsperStatsManager;
import com.ning.arecibo.aggregator.eventservice.EventServiceManager;
import com.ning.arecibo.aggregator.guice.SelfUUID;
import com.ning.arecibo.aggregator.listeners.EventRegistrationListener;
import com.ning.arecibo.aggregator.plugin.DynamicAggregatorPlugin;
import com.ning.arecibo.event.publisher.AreciboEventServiceChooser;
import com.ning.arecibo.event.publisher.EventServiceChooser;
import com.ning.arecibo.eventlogger.EventPublisher;
import com.ning.arecibo.lang.AggregationOutputProcessor;
import com.ning.arecibo.lang.Aggregator;
import com.ning.arecibo.lang.ExternalPublisher;
import com.ning.arecibo.lang.InternalDispatcher;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;

public class AggregatorRegistry extends UnicastRemoteObject implements EventRegistrationListener, RemoteAggregatorRegistry
{
	private static final Logger log = Logger.getLogger(AggregatorRegistry.class);

	private final ConcurrentHashMap<String, DynamicAggregatorPlugin> dynamicPlugins;
	private final ConcurrentHashMap<String, Aggregator> aggregators;
	private final ConcurrentHashMap<String, AggregatorImpl> impls;
	private final EventDictionary dictionary;
	private final EventServiceManager eventManager;
	private final EventPublisher publisher;
	private final AreciboEventServiceChooser chooser;
	private final EventProcessorImpl processor;
    private final EsperStatsManager esperStatsManager;
	private final UUID self;
	private final AsynchronousUpdateWorker worker;
	private final ConcurrentHashMap<String, List<AggregatorImpl>> aggregatorsPendingEventRegistration;
    private final ScheduledThreadPoolExecutor leaseExpirationExecutor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> leaseExpirationSchedFutures;


    @Inject
	public AggregatorRegistry(EventDictionary dictionary,
                           EventServiceManager eventManager,
                           EventPublisher publisher,
                           EventServiceChooser chooser,
                           EventProcessorImpl processor,
                           EsperStatsManager esperStatsManager,
                           @SelfUUID UUID self,
                           AsynchronousUpdateWorker worker) throws RemoteException
	{
		super();
		this.dictionary = dictionary;
		this.eventManager = eventManager;
		this.publisher = publisher;
        this.chooser = (AreciboEventServiceChooser)chooser;
		this.processor = processor;
        this.esperStatsManager = esperStatsManager;
		this.self = self;
		this.worker = worker;
		this.dynamicPlugins = new ConcurrentHashMap<String, DynamicAggregatorPlugin>();
		this.aggregators = new ConcurrentHashMap<String, Aggregator>();
		this.impls = new ConcurrentHashMap<String, AggregatorImpl>();
		this.aggregatorsPendingEventRegistration = new ConcurrentHashMap<String, List<AggregatorImpl>>();
		this.dictionary.addEventRegistrationListener(this);

        // TODO: inject the thread pool size here
        this.leaseExpirationExecutor = new ScheduledThreadPoolExecutor(10);
        this.leaseExpirationSchedFutures = new ConcurrentHashMap<String,ScheduledFuture<?>>();
	}

	@MonitorableManaged(monitored = true)
	public int getAggregatorCount()
	{
		return aggregators.size();
	}

    public EsperStatsManager getEsperStatsManager() {
        return this.esperStatsManager;
    }

    public synchronized void register(Aggregator agg,long leaseTime,TimeUnit leaseTimeUnit) throws RemoteException
    {
        try {
            String aggName = agg.getFullName();

            // remove any existing monitor, so can extend lease with new monitor
            removeLeaseExpirationMonitor(aggName);

            // schedule a new lease
            scheduleLeaseExpirationMonitor(aggName,leaseTime,leaseTimeUnit);

            // register
            register(agg);
        }
        catch(RuntimeException ruEx) {
            log.error(ruEx);
            throw new RemoteException("Got exception registering aggregator",ruEx);
        }
    }

	public synchronized void register(Aggregator agg) throws RemoteException
    {
        try {
            registerLocal(agg);
        }
        catch(RuntimeException ruEx) {
            log.error(ruEx);
            throw new RemoteException("Got exception registering aggregator",ruEx);
        }
    }

    public synchronized void unregister(String fullName) throws RemoteException
    {
        try {
            unregisterLocal(fullName);
        }
        catch(RuntimeException ruEx) {
            log.error(ruEx);
            throw new RemoteException("Got exception unregistering aggregator",ruEx);
        }
    }

    // this doesn't need to throw RemoteException when used internally to this class
    private void registerLocal(Aggregator agg)
    {
        registerLocal(agg,null);
    }

    private void registerLocal(Aggregator agg, DynamicAggregatorPlugin dynamicPlugin)
	{
        AggregatorImpl impl = null;
        try {
		    if (aggregators.putIfAbsent(agg.getFullName(), agg) == null) {
			    impl = new AggregatorImpl(null, agg, dynamicPlugin, this, dictionary);
			    if (impls.putIfAbsent(impl.getPath(), impl) == null) {
				    impl.register();
                }
            }
		}
        catch(RuntimeException ruEx) {
            // added explicit logging here, help track what's happening
            /*
            log.info("Got RuntimeException registering aggregator, unregistering, prior to throwing exception");
            unregisterLocal(impl);
            log.info("throwing exception now");
            */
            throw ruEx;
        }
	}

    private void unregisterLocal(String fullName) {
        AggregatorImpl impl = impls.get(fullName);
        unregisterLocal(impl);
    }

    private void unregisterLocal(AggregatorImpl impl) {
        if ( impl != null ) {
            removeLeaseExpirationMonitor(impl.getAggregator().getFullName());
            impls.remove(impl.getPath());
            aggregators.remove(impl.getAggregator().getFullName());
            impl.destroy();
        }
    }

	public List<String> getAggregatorFullNames(String namespace) throws RemoteException
	{
        try {
            List<String> list = new ArrayList<String>();
            for (Aggregator agg : aggregators.values()) {
                if ( StringUtils.equals(agg.getNamespace(), namespace) ) {
                    list.add(agg.getFullName());
                }
            }
            return list;
        }
        catch(RuntimeException ruEx) {
            log.error(ruEx);
            throw new RemoteException("Got exception getting aggregator full names",ruEx);
        }
	}

	public void ping() throws RemoteException
	{
	}

	public void registerPlugin(DynamicAggregatorPlugin da)
	{
		dynamicPlugins.putIfAbsent(da.getClass().getName(), da);
	}

    @Override
	public void eventRegistered(EventDefinition def)
	{
        // first find dynamic plugins interested in registering an aggregator from this event
		for (DynamicAggregatorPlugin dynamicPlugin : dynamicPlugins.values()) {
			Aggregator agg = dynamicPlugin.getDynamicAggregator(def);
			if (agg != null) {
                // this calls addAggregatorPendingEventRegistration for this agg, and it's children
				registerLocal(agg,dynamicPlugin);
			}
		}

        // next, do any pending requests for agg registration for child aggs that haven't been registered yet
        // remove from list here, so no infinite retry in case of exception due to bad registration
        List<AggregatorImpl> aggsToNotify = aggregatorsPendingEventRegistration.remove(def.getEventType());
        if(aggsToNotify != null) {
            // for all relevant pending aggs, invoke once, then remove from list
            for (AggregatorImpl impl : aggsToNotify) {
                try {
                    impl.notifyEventRegistered(def);
                }
                catch (Exception e) {
                    // catch exception here, so don't block other impl's in the loop from attempting registration
                    log.warn(e);

                    try {
                        // unregister here, so that it won't be stuck in a half-registered but un-notified state
                        // and allow fresh re-registration
                        unregisterLocal(impl);
                    }
                    catch (Exception e2) {
                        log.warn(e2);
                    }
                }
            }
		}
	}

    @Override
    public void eventUnRegistered(EventDefinition def)
    {
        // do nothing, the dynamicPlugins will unregister aggregators created in response to dynamic event definitions
    }

    public void removeAggregatorPendingEventRegistration(String inputEvent)
    {
        aggregatorsPendingEventRegistration.remove(inputEvent);
    }

    public void addAggregatorPendingEventRegistration(String inputEvent, AggregatorImpl aggToNotify)
	{
		EventDefinition def = dictionary.getInputEventDefintion(inputEvent);
		if ( def != null ) {
            // notify immediately if already registered
		    aggToNotify.notifyEventRegistered(def);
		}
		else {
            log.info("adding aggregator to list to be notified later on event registration for %s", inputEvent);
            List<AggregatorImpl> aggList = aggregatorsPendingEventRegistration.get(inputEvent);
            if(aggList == null) {
                aggList = new ArrayList<AggregatorImpl>();
				aggregatorsPendingEventRegistration.put(inputEvent, aggList);
			}
			aggList.add(aggToNotify);
		}
	}

	public AggregationOutputProcessorImpl createProcessor(AggregatorImpl impl, AggregationOutputProcessor p)
	{
		if (p instanceof InternalDispatcher) {
			return new InternalDispatcherImpl(impl, (InternalDispatcher) p, publisher, chooser, processor, self);
		}
		else if (p instanceof ExternalPublisher) {
			return new ExternalPublisherImpl(impl, (ExternalPublisher) p, eventManager);
		}
		throw new RuntimeException(String.format("processer %s not supported !", p.getClass()));
	}

	public void renderDebugText(PrintWriter pw)
	{
		printMap(pw, "aggregators", aggregators);
		printMap(pw, "impls", impls);
		printMap(pw, "plugins", dynamicPlugins);
		printMap(pw, "triggers", aggregatorsPendingEventRegistration);

		Set<String> nss = getAllNamespaces();

		pw.printf("\n\n--- %s ---\n\n", "EP Statements");
		for (String ns : nss) {
			EPServiceProvider ep = EsperProvider.getProvider(ns);
			TreeSet<String> sorted = new TreeSet<String>();
			for ( String stmt : ep.getEPAdministrator().getStatementNames() ) {
				sorted.add(stmt);
			}
			pw.printf("statement names \n");
			for ( String stmt : sorted ) {
				pw.printf("- %s\n", stmt);
			}
			pw.println();
			for ( String stmt : sorted ) {
				EPStatement epl = ep.getEPAdministrator().getStatement(stmt);
				pw.printf("Statement %s : %s\n\n", stmt, epl.getText());

				EventType et = epl.getEventType();
				pw.println("Statement Event Type ");
				for ( String name : et.getPropertyNames() ) {
					pw.printf("property %s , type %s\n", name, et.getPropertyType(name));
				}

			}
		}
		pw.flush();
	}

	private Set<String> getAllNamespaces()
	{
		TreeSet<String> set = new TreeSet<String>();
		for ( AggregatorImpl impl : impls.values() ) {
			set.add(impl.getAggregator().getNamespace());
		}
		return set ;
	}

	private void printMap(PrintWriter pw, String s, Map<?, ?> map)
	{
		pw.printf("\n\n--- %s ---\n\n", s);
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			pw.printf("%s = %s\n", entry.getKey(), entry.getValue());
			if (entry.getValue() instanceof AggregatorImpl) {
				((AggregatorImpl)entry.getValue()).renderDebugText(pw, 2);
			}
		}
	}

	public EPStatement registerEPL(String path, Aggregator agg)
	{
		EPServiceProvider ep = EsperProvider.getProvider(agg.getNamespace());
		EPStatement epl = ep.getEPAdministrator().createEPL(agg.getStatement(), path);
		dictionary.registerOutputEvent(agg.getOutputEvent(), epl.getEventType());
		return epl ;
	}

    public void unregisterEPL(AggregatorImpl impl) {
        EPStatement stmt = getEPL(impl);
        if (stmt != null) {
            dictionary.invalidateInputEventStreams(impl.getAggregator().getInputEvent());
            dictionary.unregisterOutputEvent(impl.getAggregator().getOutputEvent());
            stmt.destroy();
        }
    }
	
	public EPStatement getStatement(String stream)
	{
		String parts[] = stream.split("/");
		if ( parts.length > 0 ) {
			EPServiceProvider ep = EsperProvider.getProvider(parts[0]);
			return ep.getEPAdministrator().getStatement(stream);
		}
		return null;
	}

	public Set<String> getStatementNames()
	{
		TreeSet<String> list = new TreeSet<String>();
		for (EPServiceProvider ep : EsperProvider.getAllProviders()) {
			list.addAll(Arrays.asList(ep.getEPAdministrator().getStatementNames()));
		}
		return list ;
	}

	public String[] getStatementNames(String ns)
	{
		EPServiceProvider ep = EsperProvider.getProvider(ns);
		return ep.getEPAdministrator().getStatementNames() ;
	}

	public AggregatorImpl getAggregatorImpl(String name)
	{
		return impls.get(name);
	}

	public Set<String> getAggregatorNames()
	{
		return new TreeSet<String>(impls.keySet());		
	}

	public List<Aggregator> getAggregators(String namespace) throws RemoteException
	{
		List<Aggregator> list = new ArrayList<Aggregator>();
		for ( Aggregator a : aggregators.values() ) {
			if ( StringUtils.equals(a.getNamespace(), namespace) ) {
				list.add(a);
			}
		}
		return list ;
	}

	public List<Aggregator> getAggregatorsExcluding(String namespace) throws RemoteException
	{
		List<Aggregator> list = new ArrayList<Aggregator>();
		for ( Aggregator a : aggregators.values() ) {
			if ( !StringUtils.equals(a.getNamespace(), namespace) ) {
				list.add(a);
			}
		}
		return list ;
	}

    public void softRestart() throws RemoteException
    {

        try {
            log.info("doing softRestart, suspending processor");
            this.processor.suspend();

            // sleep 10 secs to make sure all in process events are cleared out
            log.info("sleeping for 10 seconds after processor suspended");
            Thread.sleep(10000L);

            this.impls.clear();
            this.aggregators.clear();
            this.aggregatorsPendingEventRegistration.clear();
            
            EsperProvider.reset();
            this.dictionary.reset();
            this.dictionary.addEventRegistrationListener(this);
            this.chooser.clearEventServiceCache();

            // sleep 10 secs to make esper engine closed down before resuming
            log.info("sleeping for 10 seconds after esper engine reset");
            Thread.sleep(10000L);
        }
        catch(Exception ex) {
            log.error(ex);
            throw new RemoteException("Got exception doing softRestart", ex);
        }
        finally {
            log.info("resuming processor");
            this.processor.resume();
        }
    }

	public EPStatement getEPL(AggregatorImpl impl)
    {
        EPServiceProvider provider = EsperProvider.getProvider(impl.getAggregator().getNamespace());
        if ( provider != null ) {
            return provider.getEPAdministrator().getStatement(impl.getPath());
        }
        return null;
    }

	public AsynchronousUpdateWorker getWorker()
	{
		return worker;
	}

    private void scheduleLeaseExpirationMonitor(String aggName,long leaseTime,TimeUnit leaseTimeUnit) {
        _LeaseExpirationMonitor mon = new _LeaseExpirationMonitor(aggName);
        ScheduledFuture<?> schedFuture = leaseExpirationExecutor.schedule(mon,leaseTime,leaseTimeUnit);
        leaseExpirationSchedFutures.put(aggName,schedFuture);
    }

    private boolean removeLeaseExpirationMonitor(String aggName) {
        ScheduledFuture<?> schedFuture = leaseExpirationSchedFutures.get(aggName);
        if(schedFuture != null && schedFuture.cancel(false)) {
            leaseExpirationSchedFutures.remove(aggName);
            return true;
        }

        return false;
    }

    private class _LeaseExpirationMonitor implements Runnable {

        final String aggName;

        public _LeaseExpirationMonitor(String aggName) {
            this.aggName = aggName;
        }

        public void run() {
            synchronized(AggregatorRegistry.this) {
                // remove our schedFuture, and if successful, unregister the aggregator
                // this lease will get removed anyway, but do here for good measure, and
                // to make sure it hasn't been done already
                boolean removed = removeLeaseExpirationMonitor(aggName);

                if(removed) {
                    try {
                        log.info("Removing aggregator due to lease expiration: %s",aggName);
                        AggregatorRegistry.this.unregisterLocal(aggName);
                    }
                    catch(Exception ex) {
                        log.warn(ex,"Exception executing LeaseExpirationMonitor");
                    }
                }
            }
        }
    }
}
