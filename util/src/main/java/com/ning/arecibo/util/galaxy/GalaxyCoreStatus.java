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

package com.ning.arecibo.util.galaxy;

import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;

public class GalaxyCoreStatus implements Comparable<GalaxyCoreStatus>
{
    public static final String RUNNING_STATUS = "running";

    private final String zoneHostName;
    private final String configPath;
    private final String runStatus;
    private final String version;
    private final String coreType;
    private final String globalZoneHostName;
    private final String ipAddress ;

    public GalaxyCoreStatus(String zoneHostName, String configPath, String runStatus, String version, String coreType, String globalZoneHostName)
    {
        this.zoneHostName = zoneHostName;
        this.configPath = configPath;
        this.runStatus = runStatus;
        this.version = version;
        this.coreType = coreType;
        this.globalZoneHostName = globalZoneHostName;
        this.ipAddress = null;
    }

    public int hashCode()
    {
        if ( zoneHostName != null ) {
            return zoneHostName.hashCode() ;
        }
        else {
            return 0;
        }
    }

    public boolean equals(Object obj)
    {
        if ( obj instanceof GalaxyCoreStatus ) {
            return StringUtils.equals(zoneHostName, ((GalaxyCoreStatus)obj).zoneHostName);
        }
        return false ;
    }

    public String getZoneHostName()
    {
        return zoneHostName;
    }

    public String getConfigPath()
    {
        return configPath;
    }

    public String getRunStatus()
    {
        return runStatus;
    }

    public String getVersion()
    {
        return version;
    }

    public String getCoreType()
    {
        return coreType;
    }

    public String getGlobalZoneHostName()
    {
        return globalZoneHostName;
    }

    public boolean isRunning()
    {
        return runStatus != null && runStatus.equals(RUNNING_STATUS);
    }

    public GalaxyCoreStatus(String line)
    {
        // crude parsing, assumes 6 white-space delimited columns, as produced by 'galaxy show'
        StringTokenizer st = new StringTokenizer(line);

        if (st.hasMoreTokens()) {
            this.zoneHostName = st.nextToken();
        }
        else {
            this.zoneHostName = null;
        }
        if (st.hasMoreTokens()) {
            this.configPath = st.nextToken();
        }
        else {
            this.configPath = null;
        }
        if (st.hasMoreTokens()) {
            this.runStatus = st.nextToken();
        }
        else {
            this.runStatus = null;
        }
        if (st.hasMoreTokens()) {
            this.version = st.nextToken();
        }
        else {
            this.version = null;
        }
        if (st.hasMoreTokens()) {
            this.coreType = st.nextToken();
        }
        else {
            this.coreType = null;
        }
        if (st.hasMoreTokens()) {
            this.globalZoneHostName = st.nextToken() ;
        }
        else {
            this.globalZoneHostName = null;
        }
        if (st.hasMoreTokens()) {
            this.ipAddress = st.nextToken() ;
        }
        else {
            this.ipAddress = null;
        }
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public String toString()
    {
        return new StringBuffer()
                .append(zoneHostName).append("\t")
                .append(configPath).append("\t")
                .append(runStatus).append("\t")
                .append(version).append("\t")
                .append(coreType).append("\t")
                .append(globalZoneHostName).append("\t")
                .append(ipAddress).toString();
    }

    public int compareTo(GalaxyCoreStatus o)
    {
        return o == null ? -1 : o.getZoneHostName().compareTo(getZoneHostName()); 
    }
}
