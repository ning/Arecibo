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

import java.util.Map;
import com.ning.arecibo.agent.config.Config;


public interface DataSource
{
	public boolean isInitialized();
    public void initialize() throws DataSourceException;
    public void closeResources() throws DataSourceException;
    public boolean canExpandConfigs();
    Map<String, Config> expandConfigs(Map<String, Config> configs) throws DataSourceException;
	public void prepareConfig(Config config) throws DataSourceException;
	public void finalizePreparation() throws DataSourceException;
	public Map<String, Object> getValues()  throws DataSourceException;
	
	public DataSourceType getDataSourceType();
}
