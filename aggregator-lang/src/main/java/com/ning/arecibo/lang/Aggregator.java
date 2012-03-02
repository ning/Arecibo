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

package com.ning.arecibo.lang;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.Stream;
import com.espertech.esper.client.soda.FilterStream;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ning.arecibo.util.Logger;

public class Aggregator implements Serializable
{
    private static final Logger log = Logger.getLoggerViaExpensiveMagic();

    static final long serialVersionUID = -5839519374219050732L;
    static final EPServiceProvider esper = EPServiceProviderManager.getProvider("compiler");

    private String inputEvent ;
	private String outputEvent ;
	private String statement ;
	private final String name ;
	private final String namespace ;
	private final List<AggregationOutputProcessor> aggregationOutputProcessors;
	private static final String DEFAULT_NS = "default";


    public Aggregator(String namespace, String name, String inputEvent)
	{
		this.namespace = namespace == null ? DEFAULT_NS : namespace ;
		this.name = name ;
		this.aggregationOutputProcessors = new ArrayList<AggregationOutputProcessor>();
		this.inputEvent = inputEvent;
	}


	public Aggregator setOutputEvent(String output)
	{
		this.outputEvent = output ;
		return this;
	}

	public Aggregator setStatement(String stmt)
	{
		this.statement = stmt ;
        EPStatementObjectModel model = esper.getEPAdministrator().compileEPL(statement);
        List<Stream> streams = model.getFromClause().getStreams();

        if (streams == null || streams.size() == 0 ) {
            throw new IllegalArgumentException("no streams in esper statement");
        }

        String stream = getStreamName(streams.get(0)) ;
        if ( stream != null ) {
            if ( !stream.equals(inputEvent) )
            {
                log.warn("inputEvent %s and statement event %s mismatch, setting inputEvent to match statement ...", inputEvent, stream);
            }
            this.inputEvent = stream ;
        }

        return this;
	}

    private String getStreamName(Stream stream)
    {
        if ( stream instanceof FilterStream ) {
            FilterStream fs = (FilterStream) stream;
            return fs.getFilter().getEventTypeName() ;
        }
        else {
            return stream.getStreamName();
        }
    }

    public Aggregator addDispatcher(DispatchRouter router, DispatcherCallback callback)
    {
        return addDispatcher(router,callback,false);
    }

    public Aggregator addDispatcher(DispatchRouter router, DispatcherCallback callback, boolean reliable)
	{
		InternalDispatcher d = new InternalDispatcher(this, router, reliable) ;
		callback.configure(d);
		addOutputProcessor(d);
		return this;
	}

	public Aggregator addOutputProcessor(AggregationOutputProcessor p)
	{
		this.aggregationOutputProcessors.add(p);
		return this;
	}

	public String getInputEvent()
	{
		return inputEvent;
	}

	public String getName()
	{
		return name;
	}

	public String getOutputEvent()
	{
		return outputEvent;
	}

	public List<AggregationOutputProcessor> getProcessors()
	{
		return Collections.unmodifiableList(aggregationOutputProcessors) ;
	}

	public String getStatement()
	{
		return statement;
	}

	public String getNamespace()
	{
		return namespace;
	}

	public String getFullName()
	{
		return getNamespace() + "/" + getName();
	}

    private Aggregator(String namespace, Aggregator other)
	{
		this.namespace = namespace;
		this.name = other.name ;
		this.aggregationOutputProcessors = other.aggregationOutputProcessors;
		this.inputEvent = other.inputEvent;
		this.outputEvent = other.outputEvent;
		this.statement = other.statement;
	}

    public static Aggregator overrideNS(String namespace, Aggregator other)
    {
        return new Aggregator(namespace, other);
    }

}
