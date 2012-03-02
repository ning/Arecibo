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

package com.ning.arecibo.aggregator.dictionary;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EventType;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.esper.EsperNamingUtils;
import com.ning.arecibo.aggregator.esper.EsperProvider;
import com.ning.arecibo.aggregator.guice.AggregatorNamespaces;
import com.ning.arecibo.aggregator.listeners.EventPreProcessorListener;
import com.ning.arecibo.aggregator.listeners.EventRegistrationListener;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.UUIDUtil;
import com.ning.arecibo.util.jmx.MonitorableManaged;

/**
 * A dictionary of Auto-generated Events
 */
public class EventDictionary implements EventPreProcessorListener
{
    private static final Logger log = Logger.getLogger(EventDictionary.class);

    final ConcurrentHashMap<String, EventDefinition> inputEvents = new ConcurrentHashMap<String, EventDefinition>();
	final ConcurrentHashMap<String, EventDefinition> outputEvents = new ConcurrentHashMap<String, EventDefinition>();
    final ConcurrentHashMap<String, List<String>> inputEventSignatures = new ConcurrentHashMap<String, List<String>>();
    final ConcurrentSkipListSet<String> inputEventRegistrationsValid = new ConcurrentSkipListSet<String>();
	final CopyOnWriteArrayList<EventRegistrationListener> listeners = new CopyOnWriteArrayList<EventRegistrationListener>();
	private final String[] namespaces;



    // TODO: this list needs to be defined by different plugins, and used on a per-plugin basis
    // TODO: this is just a temporary hack to have it by rote here
    // TODO: Or else make a system of reserved words, that all plugins can use and respect?
    private final static String[] NON_VERSION_SPECIFIC_ATTRIBUTES = {
            "datapoints",
            "eventType",
            "numHosts",
            "sourceUUID"
    };
    private final static Set<String> nonVersionSpecificAttributes = new TreeSet<String>();
    static {
        nonVersionSpecificAttributes.addAll(Arrays.asList(NON_VERSION_SPECIFIC_ATTRIBUTES));
    }

    @Inject
	public EventDictionary(@AggregatorNamespaces String[] namespaces)
	{
		this.namespaces =  namespaces;
	}

    @MonitorableManaged(monitored = true)
	public int getInputEventTypeCount()
	{
		return inputEvents.size();
	}

    @MonitorableManaged(monitored = true)
	public int getOutputEventTypeCount()
	{
		return outputEvents.size();
	}

	public void reset()
	{
		inputEvents.clear();
        inputEventSignatures.clear();
        inputEventRegistrationsValid.clear();
        outputEvents.clear();
		listeners.clear();
	}

	public String[] getNamespaces()
	{
		return namespaces;
	}


    @Override
    public void preProcessEvent(Map<String,Object> map) {
        // remove entries with null values
        Iterator<String> iter = map.keySet().iterator();
        while(iter.hasNext()) {
            if(map.get(iter.next()) == null) {
                iter.remove();
            }
        }

        // rename attributes that are esper reserved words
        EsperNamingUtils.checkWordsAreLegalEsperNamesInMapKeys(map);
    }

	// use this method if you only have the input event
    public String registerEvent(Event event, Map<String, Object> attributes)
	{
        return registerEventOrDefinition(event,attributes,null);
	}

	// use this method if you already have an EventDefinition
	public String registerDefinition(EventDefinition def) {
        return registerEventOrDefinition(null,null,def);
	}

    // use this method if you only have the input event
    private String registerEventOrDefinition(Event event, Map<String, Object> attributes, EventDefinition inputDef)
    {

        boolean gotInputDef = (inputDef != null);

        String escapedEventType;
        SortedMap<String, Class> map;

        if(gotInputDef) {
            map = inputDef.getProperties();
            escapedEventType = EsperNamingUtils.checkWordIsLegalEsperName(inputDef.getEventType());
        }
        else if(event == null || attributes == null) {
            log.warn("Got null input event or attributes in registerEventOrDefinition");
            return null;
        }
        else {
            // create map from attributes, throw out nulls
            map = new TreeMap<String, Class>();
            for ( String name : attributes.keySet() ) {
                Object attr = attributes.get(name);
                if(attr == null)
                    continue;
                Class propertyClass = attr.getClass();
                map.put(name, propertyClass);
            }
            escapedEventType = EsperNamingUtils.checkWordIsLegalEsperName(event.getEventType());
        }

        // prepare event signature
        String eventSignature = getInputEventSignature(map);

        // see if we've seen this event before at all
        if (!inputEvents.containsKey(escapedEventType)) {

            log.info("registering new event definition: %s",escapedEventType);
            addInputEventSignature(escapedEventType,eventSignature);

            // first see if we have a pre-existing outputEvent, and if so, we need to merge properties
            // in case our new upper level event is narrower than previously registered base event
            // TODO: this is just a guard for the case where we have prevOutputDef one level down
            // TODO: (it won't help in case for multi-level distance, need to replace this with plugin specific handling,
            // TODO:  to possibly find an ancestor prev def to use)
            EventDefinition prevOutputDef = this.getOutputEventDefinition(escapedEventType);
            EventDefinition newDef = null;
            if(prevOutputDef != null) {
                String msg = String.format("detected differences with previously registered outputEvent: %s",escapedEventType);
                if(logPropertyDifferences(msg,prevOutputDef.getProperties(),map)) {

                    if(gotInputDef) {
                        // make a copy of def props
                        map = new TreeMap<String, Class>();
                        map.putAll(inputDef.getProperties());
                        updateMapWithPrevOutputProperties(map,prevOutputDef.getProperties());
                        newDef = new EventDefinition(inputDef.getSourceEventClass(), escapedEventType, inputDef.getEvtClass(), map);
                    }
                    else {
                        updateMapWithPrevOutputProperties(map,prevOutputDef.getProperties());
                        newDef = new EventDefinition(event, escapedEventType, event.getClass(), map);
                    }
                    
                    String updatedEventSignature = getInputEventSignature(map);
                    addInputEventSignature(escapedEventType,updatedEventSignature);
                }
            }

            if(newDef == null) {
                if(!gotInputDef)
                    newDef = new EventDefinition(event, escapedEventType, event.getClass(), map);
                else
                    newDef = inputDef;
            }

            inputEvents.put(escapedEventType, newDef);
            registerEventStream(newDef);
        }
        else if(eventSignature != null &&  !checkInputEventSignatureRegistered(escapedEventType,eventSignature)) {
            log.info("got new signature for event type: %s",escapedEventType);

            // see if we have a new variant signature for a previously registered event
            if(gotInputDef) {
                // make a copy of def props
                map = new TreeMap<String, Class>();
                map.putAll(inputDef.getProperties());
            }

            String combinedEventSignature = null;
            EventDefinition prevDef = inputEvents.get(escapedEventType);
            String msg = String.format("detected updated event definition: %s",escapedEventType);

            if(logPropertyDifferences(msg,prevDef.getProperties(),map)) {
                map.putAll(prevDef.getProperties());
                combinedEventSignature = getInputEventSignature(map);
            }
            else {
                combinedEventSignature = eventSignature;
            }

            if(!checkInputEventSignatureRegistered(escapedEventType,combinedEventSignature)) {

                log.info("updating event definition registration: %s",escapedEventType);

                addInputEventSignature(escapedEventType,combinedEventSignature);

                EventDefinition combinedDef;
                if(gotInputDef) {
                    combinedDef = new EventDefinition(inputDef.getSourceEventClass(), escapedEventType, inputDef.getEvtClass(), map);
                }
                else {
                    combinedDef = new EventDefinition(event, escapedEventType, event.getClass(), map);
                }
                inputEvents.put(escapedEventType, combinedDef);

                updateEventStream(combinedDef);
            }
            addInputEventSignature(escapedEventType,eventSignature);
        }
        else if (!inputEventRegistrationsValid.contains(escapedEventType)) {
            // this is a previously registered stream, but no currently valid EPL statements
            log.info("revalidating event type: %s",escapedEventType);
            inputEventRegistrationsValid.add(escapedEventType);
            notifyEventRegistered(inputEvents.get(escapedEventType));
        }

        return escapedEventType;
    }

	public EventDefinition getInputEventDefinition(String inputEvent)
	{
		return inputEvents.get(inputEvent);
	}

    private String getInputEventSignature(SortedMap<String, Class> typeMap) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (SortedMap.Entry<String, Class> entry : typeMap.entrySet()) {
                md5.update(entry.getKey().getBytes());
                md5.update(entry.getValue().getSimpleName().getBytes());
            }
            byte[] d = md5.digest();

            return UUIDUtil.md5ToString(d);
        }
        catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private boolean logPropertyDifferences(String message,SortedMap<String,Class> prevMap,SortedMap<String,Class> newMap) {
        boolean foundDifference = false;
        for(String prevKey:prevMap.keySet()) {

            if(nonVersionSpecificAttributes.contains(prevKey))
                continue;

            Class prevClass = prevMap.get(prevKey);
            Class newClass = newMap.get(prevKey);

            if(prevClass != null && prevClass != newClass) {
                if(newClass == null) {
                    if(!foundDifference) {
                        log.info(message);
                        foundDifference = true;
                    }
                    log.info("\tnew event missing field in prev event: %s = %s",prevKey,prevClass.getSimpleName());
                }
                else {
                    if(!foundDifference) {
                        log.info(message);
                        foundDifference = true;
                    }
                    // should fail in subsequent esper update anyway
                    log.warn("\tnew event has new type for %s, prev: %s, new: %s",prevKey,prevClass.getSimpleName(),newClass.getSimpleName());
                }
            }
        }

        for(String newKey:newMap.keySet()) {

            if(nonVersionSpecificAttributes.contains(newKey))
                continue;

            Class newClass = newMap.get(newKey);
            Class prevClass = prevMap.get(newKey);

            if(newClass != null && prevClass != newClass) {
                if(prevClass == null) {
                    if(!foundDifference) {
                        log.info(message);
                        foundDifference = true;
                    }
                    log.info("\tnew event has new field: %s = %s",newKey,newClass.getSimpleName());
                }
            }
        }

        return foundDifference;
    }

    private boolean checkInputEventSignatureRegistered(String eventType,String eventSignature) {

        List<String> signatures = inputEventSignatures.get(eventType);

        if(signatures == null)
            return false;

        return(signatures.contains(eventSignature));
    }

    private void addInputEventSignature(String eventType,String eventSignature) {
        List<String> signatures = inputEventSignatures.get(eventType);

        if(signatures == null) {
            signatures = new ArrayList<String>();
            inputEventSignatures.put(eventType,signatures);
        }

        if(!signatures.contains(eventSignature)) {
            signatures.add(eventSignature);
        }
    }

    private void registerEventStream(EventDefinition def)
    {
        log.info("registering event type stream %s", def.getEventType());
        for ( String ns : namespaces ) {
            EPAdministrator admin = EsperProvider.getProvider(ns).getEPAdministrator();

            HashMap<String, Object> m = new HashMap<String, Object>();
            for (Map.Entry<String, Class> entry : def.getProperties().entrySet()) {
                m.put(entry.getKey(), entry.getValue());
            }

            admin.getConfiguration().addEventType(def.getEventType(), m);
            inputEventRegistrationsValid.add(def.getEventType());
        }
        notifyEventRegistered(def);
    }

	private void updateEventStream(EventDefinition def)
	{
        log.info("removing registration for event type %s", def.getEventType());
        notifyEventUnRegistered(def);

        log.info("updating event type stream %s", def.getEventType());
		for ( String ns : namespaces ) {
			EPAdministrator admin = EsperProvider.getProvider(ns).getEPAdministrator();

            HashMap<String,Object> m = new HashMap<String,Object>();
            for (Map.Entry<String, Class> entry : def.getProperties().entrySet()) {
                m.put(entry.getKey(), entry.getValue());
            }

	        admin.getConfiguration().updateMapEventType(def.getEventType(), m);
            inputEventRegistrationsValid.add(def.getEventType());
		}
		notifyEventRegistered(def);
	}

	static String toLowerCamelCase(String name)
	{
		return name.substring(0,1).toLowerCase() + name.substring(1);
	}

    private void notifyEventRegistered(EventDefinition def)
    {
        for ( EventRegistrationListener l : listeners ) {
            l.eventRegistered(def);
        }
    }

	private void notifyEventUnRegistered(EventDefinition def)
	{
		for ( EventRegistrationListener l : listeners ) {
			l.eventUnRegistered(def);
		}
	}

	// For arbitrary events
    // TODO: Need to make this aware of Esper reserved words
	public void registerEventClass(Event event)
	{
		Class evtClass = event.getClass();
		if (!inputEvents.containsKey(evtClass.getName())) {
			EventDefinition def = new EventDefinition(event, evtClass.getSimpleName(), evtClass);
			EventDefinition v = inputEvents.putIfAbsent(evtClass.getName(), def);
			if ( v == null ) {
				registerEventStream(def);
			}
		}
	}

	public void registerOutputEvent(String outputEvent, EventType eventType)
	{
        EventDefinition def = new EventDefinition(outputEvent, eventType);
		outputEvents.put(outputEvent, def);
	}

    public void unregisterOutputEvent(String outputEvent)
    {
        outputEvents.remove(outputEvent);
    }

	public void addEventRegistrationListener(EventRegistrationListener listener)
	{
		listeners.add(listener);
	}

    public EventDefinition getInputEventDefintion(String name)
    {
        if(inputEventRegistrationsValid.contains(name))
            return inputEvents.get(name);
        else
            return null;
    }

    public void invalidateInputEventStreams(String name)
    {
        inputEventRegistrationsValid.remove(name);
    }

    private void updateMapWithPrevOutputProperties(Map<String,Class> map, Map<String,Class> prevOutputProperties) {
        for(String prevOutputPropertyKey:prevOutputProperties.keySet()) {
            if(nonVersionSpecificAttributes.contains(prevOutputPropertyKey))
                continue;

            if(!map.containsKey(prevOutputPropertyKey)) {
                map.put(prevOutputPropertyKey,prevOutputProperties.get(prevOutputPropertyKey));
            }
        }
    }

    public EventDefinition getOutputEventDefinition(String name)
	{
		return outputEvents.get(name);
	}

	public EventDefinition getEventDefintion(String name)
	{
		EventDefinition def = inputEvents.get(name);
		if ( def != null ) {
			return def ;
		}
		else {
			return outputEvents.get(name);
		}
	}

	public void renderDebugText(PrintWriter pw)
	{
		pw.printf("-- Event Dictionary --\n\n");
		pw.printf("-- Input Events --\n\n");
		for ( Map.Entry<String, EventDefinition> entry : inputEvents.entrySet() ) {
			entry.getValue().renderDebugText(pw) ;								
		}

		pw.printf("-- Output Events --\n\n");
		for ( Map.Entry<String, EventDefinition> entry : outputEvents.entrySet() ) {
			entry.getValue().renderDebugText(pw) ;
		}

		pw.flush();
	}

	public Set<String> getEventNames()
	{
		TreeSet<String> set = new TreeSet<String>();
		set.addAll(inputEvents.keySet());
		set.addAll(outputEvents.keySet());
		return set ;
	}
}
