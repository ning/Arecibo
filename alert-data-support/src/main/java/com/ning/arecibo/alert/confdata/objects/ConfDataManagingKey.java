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

package com.ning.arecibo.alert.confdata.objects;

import com.ning.arecibo.alert.confdata.enums.ManagingKeyActionType;
import com.ning.arecibo.util.Logger;

import java.sql.Timestamp;
import java.util.Map;

public class ConfDataManagingKey extends ConfDataObject
{
    private static final Logger log = Logger.getLogger(ConfDataManagingKey.class);

    public static final String TYPE_NAME = "managing_key";
    public static final String INSERT_TEMPLATE_NAME = ":insert_managing_key";
    public static final String UPDATE_TEMPLATE_NAME = ":update_managing_key";

    private static final String KEY_FIELD = "key";
    private static final String ACTION_FIELD = "action";
    private static final String ACTIVATED_INDEFINITELY_FIELD = "activated_indefinitely";
    private static final String ACTIVATED_UNTIL_TS_FIELD = "activated_until_ts";
    private static final String AUTO_ACTIVATE_TOD_START_MS = "auto_activate_tod_start_ms";
    private static final String AUTO_ACTIVATE_TOD_END_MS = "auto_activate_tod_end_ms";
    private static final String AUTO_ACTIVATE_DOW_START = "auto_activate_dow_start";
    private static final String AUTO_ACTIVATE_DOW_END = "auto_activate_dow_end";

    protected volatile String key = null;
    protected volatile ManagingKeyActionType action = null;
    protected volatile Boolean activatedIndefinitely = null;
    protected volatile Timestamp activatedUntilTs = null;

    // Time Of Day, based on GMT
    protected volatile Long autoActivateTODStartMs = null;
    protected volatile Long autoActivateTODEndMs = null;

    // Day Of Week, 1-based, Monday is 1
    // (based on Joda Time DateTimeConstants)
    protected volatile Long autoActivateDOWStart = null;
    protected volatile Long autoActivateDOWEnd = null;

    public ConfDataManagingKey()
    {
    }

    @Override
    public String getTypeName()
    {
        return TYPE_NAME;
    }

    @Override
    public String getInsertSqlTemplateName()
    {
        return INSERT_TEMPLATE_NAME;
    }

    @Override
    public String getUpdateSqlTemplateName()
    {
        return UPDATE_TEMPLATE_NAME;
    }

    @Override
    public void setPropertiesFromMap(final Map<String, Object> map)
    {
        super.setPropertiesFromMap(map);
        setKey(getString(map, KEY_FIELD));
        setAction(getEnum(map, ACTION_FIELD, ManagingKeyActionType.class));
        setActivatedIndefinitely(getBoolean(map, ACTIVATED_INDEFINITELY_FIELD));
        setActivatedUntilTs(getTimestamp(map, ACTIVATED_UNTIL_TS_FIELD));
        setAutoActivateTODStartMs(getLong(map, AUTO_ACTIVATE_TOD_START_MS));
        setAutoActivateTODEndMs(getLong(map, AUTO_ACTIVATE_TOD_END_MS));
        setAutoActivateDOWStart(getLong(map, AUTO_ACTIVATE_DOW_START));
        setAutoActivateDOWEnd(getLong(map, AUTO_ACTIVATE_DOW_END));
    }

    @Override
    public Map<String, Object> toPropertiesMap()
    {
        final Map<String, Object> map = super.toPropertiesMap();
        setString(map, KEY_FIELD, getKey());
        setEnum(map, ACTION_FIELD, getAction());
        setBoolean(map, ACTIVATED_INDEFINITELY_FIELD, getActivatedIndefinitely());
        setTimestamp(map, ACTIVATED_UNTIL_TS_FIELD, getActivatedUntilTs());
        setLong(map, AUTO_ACTIVATE_TOD_START_MS, getAutoActivateTODStartMs());
        setLong(map, AUTO_ACTIVATE_TOD_END_MS, getAutoActivateTODEndMs());
        setLong(map, AUTO_ACTIVATE_DOW_START, getAutoActivateDOWStart());
        setLong(map, AUTO_ACTIVATE_DOW_END, getAutoActivateDOWEnd());

        return map;
    }

    @Override
    public void toStringBuilder(final StringBuilder sb)
    {
        super.toStringBuilder(sb);
        sb.append(String.format("   %s -> %s\n", KEY_FIELD, getKey()));
        sb.append(String.format("   %s -> %s\n", ACTION_FIELD, getAction()));
        sb.append(String.format("   %s -> %s\n", ACTIVATED_INDEFINITELY_FIELD, getActivatedIndefinitely()));
        sb.append(String.format("   %s -> %s\n", ACTIVATED_UNTIL_TS_FIELD, getActivatedUntilTs()));
        sb.append(String.format("   %s -> %s\n", AUTO_ACTIVATE_TOD_START_MS, getAutoActivateTODStartMs()));
        sb.append(String.format("   %s -> %s\n", AUTO_ACTIVATE_TOD_END_MS, getAutoActivateTODEndMs()));
        sb.append(String.format("   %s -> %s\n", AUTO_ACTIVATE_DOW_START, getAutoActivateDOWStart()));
        sb.append(String.format("   %s -> %s\n", AUTO_ACTIVATE_DOW_END, getAutoActivateDOWEnd()));
    }

    public String getKey()
    {
        return this.key;
    }

    public void setKey(final String key)
    {
        this.key = key;
    }

    public ManagingKeyActionType getAction()
    {
        return this.action;
    }

    public void setAction(final ManagingKeyActionType action)
    {
        this.action = action;
    }

    public Boolean getActivatedIndefinitely()
    {
        return activatedIndefinitely;
    }

    public void setActivatedIndefinitely(final Boolean activatedIndefinitely)
    {
        this.activatedIndefinitely = activatedIndefinitely;
    }

    public Timestamp getActivatedUntilTs()
    {
        return this.activatedUntilTs;
    }

    public void setActivatedUntilTs(final Timestamp activatedUntilTs)
    {
        this.activatedUntilTs = activatedUntilTs;
    }

    public Long getAutoActivateTODStartMs()
    {
        return autoActivateTODStartMs;
    }

    public void setAutoActivateTODStartMs(final Long autoActivateTODStartMs)
    {
        this.autoActivateTODStartMs = autoActivateTODStartMs;
    }

    public Long getAutoActivateTODEndMs()
    {
        return autoActivateTODEndMs;
    }

    public void setAutoActivateTODEndMs(final Long autoActivateTODEndMs)
    {
        this.autoActivateTODEndMs = autoActivateTODEndMs;
    }

    public Long getAutoActivateDOWStart()
    {
        return autoActivateDOWStart;
    }

    public void setAutoActivateDOWStart(final Long autoActivateDOWStart)
    {
        this.autoActivateDOWStart = autoActivateDOWStart;
    }

    public Long getAutoActivateDOWEnd()
    {
        return autoActivateDOWEnd;
    }

    public void setAutoActivateDOWEnd(final Long autoActivateDOWEnd)
    {
        this.autoActivateDOWEnd = autoActivateDOWEnd;
    }
}
