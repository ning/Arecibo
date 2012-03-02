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

package com.ning.arecibo.aggregator.stringtemplates;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.apache.log4j.Logger;

public class HtmlTemplateGroupLoader
{
	private static final Logger log = Logger.getLogger(HtmlTemplateGroupLoader.class);

	public static StringTemplateGroup load(String name)
	{
		return load(name, HtmlTemplateGroupLoader.class.getClassLoader().getResource(name));
	}

	public static List<StringTemplateGroup> loadAll(String name)
	{
		try {
			Enumeration<URL> resourceUrls = HtmlTemplateGroupLoader.class.getClassLoader().getResources(name);
			List<StringTemplateGroup> groups = new ArrayList<StringTemplateGroup>();

			while (resourceUrls.hasMoreElements()) {
				groups.add(load(name, resourceUrls.nextElement()));
			}
			return groups;
		}
		catch (IOException ex) {
			throw new RuntimeException(String.format("Error loading StringTemplate: %s", name), ex);
		}
	}

	public static StringTemplateGroup load(String name, URL resourceUrl)
	{
		Reader reader;

		try {
			reader = new InputStreamReader(resourceUrl.openStream());
		}
		catch (IOException ex) {
			throw new RuntimeException(String.format("Error loading StringTemplate: %s", name), ex);
		}

		AtomicBoolean error = new AtomicBoolean(false);

		StringTemplateGroup result = new StringTemplateGroup(reader, DefaultTemplateLexer.class, new StringTemplateErrorListener()
		{
			public void error(String msg, Throwable e)
			{
				log.error(msg, e);
			}

			public void warning(String msg)
			{
				log.warn(msg);
			}

			public void debug(String msg)
			{
				log.debug(msg);
			}
		});

		if (error.get()) {
			throw new RuntimeException(String.format("Error loading StringTemplate: %s", name));
		}

		return result;
	}

	public static StringTemplateGroup load(String namespace, String name)
	{
		return load(namespace.replace(".", "/") + name);
	}

	public static List<StringTemplateGroup> loadAll(String namespace, String name)
	{
		return loadAll(namespace.replace(".", "/") + name);
	}

	public static StringTemplateGroup load(Class classObj, String name)
	{
		return load(classObj.getPackage().getName(), name);
	}

	public static List<StringTemplateGroup> loadAll(Class classObj, String name)
	{
		return loadAll(classObj.getPackage().getName(), name);
	}
}


