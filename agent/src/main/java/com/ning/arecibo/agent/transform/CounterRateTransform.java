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

package com.ning.arecibo.agent.transform;

import com.ning.arecibo.util.Logger;

public class CounterRateTransform extends Transform
{
	private static final Logger log = Logger.getLogger(CounterRateTransform.class);

	Long lastUpdate;
	Object lastValue;

	/**
	 * Calculate the rate of change of the given value. As this function is called repeatedly, each value is compared
	 * to the previous one. The time difference between calls, ie between polling events, is used to calculate the
	 * change rate in seconds.
	 *
	 * This transform assumes data is always monotonically increasing, and detects discontinuities due to precision wrapping.
	 * If the rate shows a spurious negative value, no value is emitted, but the lastUpdate/lastValue are updated.
	 */
	@Override
	public Object process(Object value)
	{
		Long time = System.currentTimeMillis();
		if (this.lastValue == null) {
			this.lastUpdate = time;
			this.lastValue = value;
			return null;					        // no value at this time
		}
		if (!(value instanceof Number)) {
			throw new IllegalArgumentException(String.format("Rate requested, but value '%s' is not a number", value.toString()));
		}
		double diff = ((Number) value).doubleValue() - ((Number) this.lastValue).doubleValue();
		double timeChangeInSeconds = ((double)(time - this.lastUpdate)) / 1000.0;
		
		double rate;
		if(timeChangeInSeconds == 0.0)
		    rate = Double.NaN;
		else
		    rate = diff / timeChangeInSeconds;

		if (Double.isNaN(rate) || rate == Double.POSITIVE_INFINITY || rate == Double.NEGATIVE_INFINITY || rate < 0.0) {
			log.info("Overflow in rate calculation: val=%f, last val=%f; time=%tc, last time=%tc", ((Number) value).doubleValue(), ((Number) this.lastValue).doubleValue(), time, this.lastUpdate);
			this.lastUpdate = time;
			this.lastValue = value;
			return null;					        // no value at this time
		}

		// success. update last values before returning
		this.lastUpdate = time;
		this.lastValue = value;
		return rate;
	}

}
