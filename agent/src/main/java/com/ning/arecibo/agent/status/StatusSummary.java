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
