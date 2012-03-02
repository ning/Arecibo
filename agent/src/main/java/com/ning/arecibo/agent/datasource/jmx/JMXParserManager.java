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

package com.ning.arecibo.agent.datasource.jmx;

import com.ning.arecibo.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ning.arecibo.agent.datasource.ValueParser;

public final class JMXParserManager
{
	private static final Logger log = Logger.getLogger(JMXParserManager.class);

	private final Map<String, ValueParser> parserMap;

	public JMXParserManager()
	{
		LogLevelCountsParser logLevelCountsParser = new LogLevelCountsParser();

		this.parserMap = new HashMap<String, ValueParser>();

        // this is a hack, we shouldn't build knowledge of specific attributes
        // in here, it should be done at the configuration level, etc.
		this.parserMap.put("LogLevelCounts", logLevelCountsParser);
	}

    public void addParsers(MonitoredMBean monitoredMBean)
	{
		String[] priorAttrsArray = monitoredMBean.getRelevantAttributes();
		List<String> relevantAttrs = new ArrayList<String>(Arrays.asList(priorAttrsArray));

		for (String attribute : relevantAttrs) {

			// add the parser
			ValueParser parser = this.parserMap.get(attribute);
			if (parser != null) {
				monitoredMBean.addValueParser(attribute, parser);
			}
		}
	}
}
