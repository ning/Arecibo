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
