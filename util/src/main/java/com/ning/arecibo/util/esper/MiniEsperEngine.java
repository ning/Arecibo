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

package com.ning.arecibo.util.esper;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;

import java.util.Iterator;

public class MiniEsperEngine<T extends Number>
{
    private final EPServiceProvider provider;
    private final EPStatement avgEPL;
    private final EPStatement aggEPL;
    private final String interval;

    public MiniEsperEngine(String eventType, Class<? extends T> clazz)
    {
        this(eventType, clazz, null);
    }

    public MiniEsperEngine(String eventType, Class<? extends T> clazz, String inv)
    {
        this.interval = inv == null ? "1 min" : inv;

        String valueMethod;
        if(clazz == Long.class) {
            valueMethod = "longValue";
        }
        else {
            valueMethod = "doubleValue";
        }

        Configuration config = new Configuration();
        ConfigurationEventTypeLegacy def = new ConfigurationEventTypeLegacy();
        def.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        config.addEventType(eventType, clazz.getName(), def);
        provider = EPServiceProviderManager.getProvider(eventType, config);


        avgEPL = createStatement(String.format("select avg(%s) as avgV from %s.win:time(%s)", valueMethod, eventType, interval));
        aggEPL = createStatement(String.format("select max(%s) as maxV, min(%s) as minV, count(*) as countV from %s", valueMethod, valueMethod, eventType));
    }

    protected EPStatement createStatement(String stmt)
    {
        return provider.getEPAdministrator().createEPL(stmt);
    }

    public void send(T object)
    {
        provider.getEPRuntime().sendEvent(object);
    }

    private Number getNumberFromEPL(EPStatement epl, String name)
    {
        Iterator<EventBean> i = epl.iterator();
        if ( i.hasNext() ) {
            EventBean b = i.next();
            Number l = (Number) b.get(name);
            if ( l != null ) {
                return l ;
            }
        }
        return -1L;
    }

    public long getCount()
    {
        return getNumberFromEPL(aggEPL, "countV").longValue();
    }

    public Number getMin()
    {
        return getNumberFromEPL(aggEPL, "minV");
    }

    public Number getMax()
    {
        return getNumberFromEPL(aggEPL, "maxV");
    }

    public double getAverage()
    {
        return getNumberFromEPL(avgEPL, "avgV").doubleValue();
    }
}
