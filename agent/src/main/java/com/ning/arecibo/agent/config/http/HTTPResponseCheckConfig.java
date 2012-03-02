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

package com.ning.arecibo.agent.config.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.skife.config.TimeSpan;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.ConfigType;
import com.ning.arecibo.agent.guice.GuiceDefaultsForDataSources;

public class HTTPResponseCheckConfig extends Config {

    public final static String PORT = "port";
    public final static String URI = "uri";
    public final static String VIRTUAL_HOST = "virtualHost";
    public final static String EXTRA_HEADERS = "extraHeaders";
    public final static String IGNORE_BODY_FLAG = "ignoreBody";
    public final static String FOLLOW_REDIRECTS_FLAG = "followRedirects";
    public final static String MATCH_STATUS_MESSAGE_SUB_STRING = "matchStatusMessageSubString";
    public final static String MATCH_BODY_SUB_STRING = "matchBodySubString";

    protected final String uri;
    protected final boolean ignoreBodyFlag;
    protected final boolean followRedirectsFlag;
    protected final int port;
    protected final String virtualHost;
    protected final Map<String, Collection<String>> extraHeaders;
    protected final String matchStatusMessageSubString;
    protected final String matchBodySubString;

    public HTTPResponseCheckConfig(String host,
                                   String fullConfigPath,
                                   String deployedType,
                                   GuiceDefaultsForDataSources guiceDefaults,
                                   Map<String,Object> optionsMap) throws ConfigException {

        super((String) optionsMap.get(Config.EVENT_TYPE),
                Config.EVENT_ATTRIBUTE_TYPE_VIRTUAL,
                null,
                optionsMap.get(Config.POLLING_INTERVAL_SECS) != null ?
                    new TimeSpan(optionsMap.get(Config.POLLING_INTERVAL_SECS).toString()) :
                    guiceDefaults.getDefaultPollingInterval(),
                host,
                fullConfigPath,
                deployedType);

        Object portObj = optionsMap.get(PORT);
        if(portObj == null) {
            this.port = 80;
        }
        else {
            this.port = Integer.parseInt(portObj.toString());
        }

        String uriString = (String)optionsMap.get(URI);
        if(uriString == null)
            uriString = "/";
        this.uri = uriString;

        this.virtualHost = (String)optionsMap.get(VIRTUAL_HOST);

        List extraHeadersList = (List)optionsMap.get(EXTRA_HEADERS);
        if(extraHeadersList != null) {
            this.extraHeaders = new LinkedHashMap<String, Collection<String>>();
            for(Object header:extraHeadersList) {
                String headerString = (String)header;
                String[] parts = headerString.split(":");
                String name = parts[0].trim();
                String value = parts[1].trim();
                Collection<String> values = this.extraHeaders.get(name);
                if (values == null) {
                    values = new ArrayList<String>();
                    this.extraHeaders.put(name, values);
                }
                values.add(value);
            }
        }
        else
            this.extraHeaders = null;

        String noBodyFlagString = (String)optionsMap.get(IGNORE_BODY_FLAG);
        if(noBodyFlagString == null)
            this.ignoreBodyFlag = false;
        else
            this.ignoreBodyFlag = Boolean.valueOf(noBodyFlagString);

        String followRedirectsFlagString = (String)optionsMap.get(FOLLOW_REDIRECTS_FLAG);
        if(followRedirectsFlagString == null)
            this.followRedirectsFlag = false;
        else
            this.followRedirectsFlag = Boolean.valueOf(followRedirectsFlagString);

        String subString = (String)optionsMap.get(MATCH_STATUS_MESSAGE_SUB_STRING);
        if(subString != null)
            this.matchStatusMessageSubString = subString;
        else
            this.matchStatusMessageSubString = null;

        subString = (String)optionsMap.get(MATCH_BODY_SUB_STRING);
        if(subString != null)
            this.matchBodySubString = subString;
        else
            this.matchBodySubString = null;
    }

    public HTTPResponseCheckConfig(HTTPResponseCheckConfig orig, String newEventAttributeType) throws ConfigException {
        super(orig,orig.eventType, newEventAttributeType);
        this.port = orig.port;
        this.uri = orig.uri;
        this.ignoreBodyFlag = orig.ignoreBodyFlag;
        this.followRedirectsFlag = orig.followRedirectsFlag;
        this.virtualHost = orig.virtualHost;
        this.extraHeaders = orig.extraHeaders;
        this.matchStatusMessageSubString = orig.matchStatusMessageSubString;
        this.matchBodySubString = orig.matchBodySubString;
    }

    public int getPort() {
        return port;
    }
    
    public String getUri() {
        return this.uri;
    }

    public boolean getIgnoreBodyFlag() {
        return this.ignoreBodyFlag;
    }

    public boolean getFollowRedirectsFlag() {
        return this.followRedirectsFlag;
    }

    public String getVirtualHost() {
        return this.virtualHost;
    }

    public Map<String, Collection<String>> getExtraHeaders() {
        return this.extraHeaders;
    }

    public String getMatchStatusMessageSubString() {
        return this.matchStatusMessageSubString;
    }

    public String getMatchBodySubString() {
        return this.matchBodySubString;
    }

    @Override
    public String getConfigDescriptor() {

        return super.getConfigDescriptor() + ":" +
                this.port + ":" + this.uri + ":" + this.ignoreBodyFlag + ":" + this.followRedirectsFlag;
    }

    @Override
    public String getConfigHashKey() {

        return this.port + ":" + this.uri + ":" + this.ignoreBodyFlag + ":" + this.followRedirectsFlag + ":" +
                super.getConfigHashKey();
    }

    @Override
    public boolean equalsConfig(Config cmpConfig) {

        if (cmpConfig == null || !(cmpConfig instanceof HTTPResponseCheckConfig))
            return false;

        if (!super.equalsConfig(cmpConfig))
            return false;

        if (!StringUtils.equals(this.uri, ((HTTPResponseCheckConfig) cmpConfig).uri))
            return false;
        if (this.ignoreBodyFlag != ((HTTPResponseCheckConfig) cmpConfig).ignoreBodyFlag)
            return false;
        if (this.followRedirectsFlag != ((HTTPResponseCheckConfig) cmpConfig).followRedirectsFlag)
            return false;

        return true;
    }

    @Override
    public ConfigType getConfigType() {
        return ConfigType.HTTP;
    }
}
