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

package com.ning.arecibo.agent.datasource.tcp;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.tcp.TCPConnectCheckConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;

import com.ning.arecibo.util.Logger;

public class TCPConnectCheckDataSource implements DataSource {

    private static final Logger log = Logger.getLogger(TCPConnectCheckDataSource.class);

    public static final String CONNECT_CHECK_RESULT = "connectCheckResult";
    public static final String CONNECT_TEST_TIME_MS = "connectTestTimeMs";
    public static final String CONNECT_TEST_MESSAGE = "connectTestMessage";

    public static final String SUCCESSFUL_CONNECT_MESSAGE = "OK";
    public static final int SUCCESSFUL_CONNECT_RESULT = 1;
    public static final int FAILED_CONNECT_RESULT = -1;

    // 10 ^ 6
    public static final double NANOS_PER_MILLI = 1000000.0;

    private final String host;
    private final int port;
    private final TimeSpan timeout;

    private final Map<String, String> configHashKeyMap;

    public TCPConnectCheckDataSource(Config config, TimeSpan timeout)
        throws DataSourceException {

        if(!(config instanceof TCPConnectCheckConfig)) {
            throw new DataSourceException("TCPConnectCheckDataSource must be initialized with an instance of TCPConnectCheckConfig");
        }
        TCPConnectCheckConfig tcpConnectCheckConfig = (TCPConnectCheckConfig)config;

        this.host = config.getHost();
        this.port = tcpConnectCheckConfig.getPort();
        this.timeout = timeout;

        this.configHashKeyMap = new HashMap<String,String>();
    }

    @Override
    public void initialize() throws DataSourceException {
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void closeResources() throws DataSourceException {
    }

    @Override
    public boolean canExpandConfigs() {
        return true;
    }

    @Override
    public Map<String, Config> expandConfigs(Map<String, Config> configs) throws DataSourceException {

        // for each config, create 1 for each attribute type we want to include
        List<Config> toAdd = new ArrayList<Config>();
        for(Config config:configs.values()) {
            try {
                toAdd.add(new TCPConnectCheckConfig((TCPConnectCheckConfig)config,CONNECT_CHECK_RESULT));
                toAdd.add(new TCPConnectCheckConfig((TCPConnectCheckConfig)config,CONNECT_TEST_TIME_MS));
                toAdd.add(new TCPConnectCheckConfig((TCPConnectCheckConfig)config,CONNECT_TEST_MESSAGE));
            }
            catch(ConfigException cEx) {
                throw new DataSourceException("Problem expanding configs",cEx);
            }
        }

        configs.clear();
        for(Config config:toAdd) {
            configs.put(config.getConfigHashKey(),config);
        }

        return configs;
    }

    @Override
    public void prepareConfig(Config config) throws DataSourceException {
        this.configHashKeyMap.put(config.getEventAttributeType(),config.getConfigHashKey());
    }

    @Override
    public void finalizePreparation() throws DataSourceException {
    }

    @Override
    public Map<String, Object> getValues()  throws DataSourceException {

        final long startNanos = System.nanoTime();

        final HashMap<String,Object> values = new HashMap<String,Object>();

        Socket sock = null;
        try {
            SocketAddress sockAddr = new InetSocketAddress(host,port);

            sock = new Socket();
            sock.connect(sockAddr, (int)timeout.getMillis());

            values.put(configHashKeyMap.get(CONNECT_CHECK_RESULT), SUCCESSFUL_CONNECT_RESULT);
            values.put(configHashKeyMap.get(CONNECT_TEST_MESSAGE), SUCCESSFUL_CONNECT_MESSAGE);
        }
        catch(Exception ex) {
            values.put(configHashKeyMap.get(CONNECT_CHECK_RESULT), FAILED_CONNECT_RESULT);

            Throwable t = ex;
            while (t.getCause() != null)
                t = t.getCause();

            values.put(configHashKeyMap.get(CONNECT_TEST_MESSAGE), t.toString());
        }
        finally {
            // get the timing for the connect
            long endNanos = System.nanoTime();
            double connectMillis = (double) (endNanos - startNanos) / NANOS_PER_MILLI;
            values.put(configHashKeyMap.get(CONNECT_TEST_TIME_MS), connectMillis);

            if(sock != null) {
                try {
                    sock.close();
                }
                catch(IOException ioEx) {
                    log.warn(ioEx);
                }
            }
        }

        return values;
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.TCPConnectCheck;
    }

    public static boolean matchesConfig(Config config) {
        return config instanceof TCPConnectCheckConfig;
    }

}
