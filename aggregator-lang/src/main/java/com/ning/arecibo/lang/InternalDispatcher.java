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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class InternalDispatcher implements AggregationOutputProcessor, Serializable
{
	static final long serialVersionUID = 4930192480414372461L;
	
	private final DispatchRouter router;
	private final List<Aggregator> aggregators ;
	private final boolean reliable;
	private final Aggregator parent;

    public InternalDispatcher(Aggregator parent, DispatchRouter router)
    {
        this(parent, router,false);
    }

    public InternalDispatcher(Aggregator parent, DispatchRouter router, boolean reliable)
	{
		this.router = router;
		this.aggregators = new ArrayList<Aggregator>();
		this.parent = parent ;
        this.reliable = reliable;
	}

    public InternalDispatcher addAggregator(String name, AggregatorCallback callback)
	{
		Aggregator agg = new Aggregator(parent.getNamespace(), name, parent.getOutputEvent());
		this.aggregators.add(agg);
		callback.configure(agg);
		return this;
	}

	public List<Aggregator> getAggregators()
	{
		return Collections.unmodifiableList(aggregators);
	}
	
	public DispatchRouter getRouter()
	{
		return router;
	}

	public boolean isReliable()
	{
		return reliable ;
	}
}
