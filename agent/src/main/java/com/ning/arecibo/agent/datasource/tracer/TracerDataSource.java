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

package com.ning.arecibo.agent.datasource.tracer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.tracer.TracerConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;


public class TracerDataSource implements DataSource
{
    public final static String SINE_ONE_HOUR = "sineOneHour";
    public final static String COSINE_ONE_HOUR = "cosineOneHour";
    public final static String SINE_THRESHOLD_ONE_HOUR = "sineThresholdOneHour";
    public final static String COSINE_THRESHOLD_HALF_HOUR = "cosineThresholdHalfHour";
    
    public final static long HALF_HOUR = 1000L * 60L * 30L;
    public final static long ONE_HOUR = 1000L * 60L * 60L;
    
    public final static double ONE_HOUR_PERIOD_FACTOR = (2.0 * Math.PI) / (double)ONE_HOUR;
    public final static double HALF_HOUR_PERIOD_FACTOR = (2.0 * Math.PI) / (double)HALF_HOUR;
    
    private final List<String> methodTypes;
    private final Map<String,String> configHashKeyMap;
    
    public TracerDataSource(Config config) throws DataSourceException {

        if(!(config instanceof TracerConfig)) {
            throw new DataSourceException("TracerDataSource must be initialized with an instance of TracerConfig");
        }

        methodTypes = new ArrayList<String>();
        configHashKeyMap = new HashMap<String,String>();
    }

    @Override
    public void finalizePreparation()
    {
    }

    @Override
    public void closeResources()
    {
    }

    @Override
    public void initialize()
    {
    }

    @Override
    public boolean isInitialized()
    {
        return true;
    }

    @Override
    public boolean canExpandConfigs() {
        return false;
    }

    @Override
    public Map<String, Config> expandConfigs(Map<String, Config> configs) {
        throw new IllegalStateException("expandConfigs not supported");
    }

    @Override
    public void prepareConfig(Config config) throws DataSourceException
    {
        TracerConfig tracerConfig;
        if (config instanceof TracerConfig) {
            tracerConfig = (TracerConfig) config;
        }
        else {
            // shouldn't happen
            throw new DataSourceException("Passed in config is not an instanceof TracerConfig");
        }
        methodTypes.add(tracerConfig.getMethodType());
        configHashKeyMap.put(tracerConfig.getMethodType(),config.getConfigHashKey());
    }

    @Override
    public Map<String, Object> getValues()
    {
        HashMap<String,Object> retMap = new HashMap<String,Object>();

        for(String methodType:methodTypes) {
            if(methodType.equals(SINE_ONE_HOUR)) {
                Double value = sineOneHourPeriod(System.currentTimeMillis());
                retMap.put(configHashKeyMap.get(methodType), value);
            }
            else if(methodType.equals(COSINE_ONE_HOUR)) {
                Double value = cosineOneHourPeriod(System.currentTimeMillis());
                retMap.put(configHashKeyMap.get(methodType), value);
            }
            else if(methodType.equals(SINE_THRESHOLD_ONE_HOUR)) {
                Double value = sineThresholdOneHourPeriod(System.currentTimeMillis());
                retMap.put(configHashKeyMap.get(methodType), value);
            }
            else if(methodType.equals(COSINE_THRESHOLD_HALF_HOUR)) {
                Double value = cosineThresholdHalfHourPeriod(System.currentTimeMillis());
                retMap.put(configHashKeyMap.get(methodType), value);
            }
        }

        return retMap;
    }

    private double sineOneHourPeriod(long millis) {
        return Math.sin((double)millis * ONE_HOUR_PERIOD_FACTOR);
    }
    
    private double cosineOneHourPeriod(long millis) {
        return Math.cos((double)millis * ONE_HOUR_PERIOD_FACTOR);
    }
    
    private double cosineHalfHourPeriod(long millis) {
        return Math.cos((double)millis * HALF_HOUR_PERIOD_FACTOR);
    }
    
    private double sineThresholdOneHourPeriod(long millis) {
        double sinVal = sineOneHourPeriod(millis);
        if(sinVal >= 0)
            return 1.0;
        else
            return -1.0;
    }
    
    private double cosineThresholdHalfHourPeriod(long millis) {
        double cosVal = cosineHalfHourPeriod(millis);
        if(cosVal >= 0)
            return 1.0;
        else
            return -1.0;
    }

	public static boolean matchesConfig(Config config) {
        return config instanceof TracerConfig;
	}

	@Override
	public DataSourceType getDataSourceType() {
		return DataSourceType.Tracer;
	}
}
