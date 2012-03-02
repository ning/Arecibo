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

package com.ning.arecibo.agent.datasource;

import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigIterator;

public class IdentityConfigIterator implements ConfigIterator {
	
	private final Config config;
	private volatile boolean allocated = false;
	
	public IdentityConfigIterator(Config config) {
		this.config = config;
	}
	
	public Config getNextConfig() throws DataSourceException {
		if(!allocated) {
			allocated = true;
			return config;
		}
		else
			return null;
	}
}
