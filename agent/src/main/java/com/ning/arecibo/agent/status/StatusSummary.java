package com.ning.arecibo.agent.status;

/**
 * StatusSummary
 * <p/>
 * <p/>
 * <p/>
 * Author: gary
 * Date: Jul 9, 2008
 * Time: 10:44:21 AM
 */
public final class StatusSummary
{
	private final long timestamp;
	private final String zone;
	private final int numAccessors;
	private final int numSuccesses;
	private final int numQueries;

	public StatusSummary(long timestamp, String zone, int numAccessors, int numSuccesses, int numQueries)
	{
		this.timestamp = timestamp;
		this.zone = zone;
		this.numAccessors = numAccessors;
		this.numSuccesses = numSuccesses;
		this.numQueries = numQueries;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public String getZone()
	{
		return zone;
	}

	public int getNumAccessors()
	{
		return numAccessors;
	}

	public int getNumSuccesses()
	{
		return numSuccesses;
	}

	public double getPercentSuccessful() {
		if (this.numQueries == 0) {
			return 0.0;
		}
		return 100.0 * this.numSuccesses / (double) this.numQueries;
	}

	public int getNumQueries()
	{
		return numQueries;
	}
}
