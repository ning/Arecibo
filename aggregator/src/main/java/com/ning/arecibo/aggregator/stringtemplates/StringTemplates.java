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

import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

public class StringTemplates
{
	public static final StringTemplateGroup html = HtmlTemplateGroupLoader.load(StringTemplates.class, "/html.st");

	public static StringTemplate getTemplate(String name)
	{
		return html.getInstanceOf(name);	
	}

	public static StringTemplate getTemplate(String name, Object... attrs)
	{
		StringTemplate st = html.getInstanceOf(name);
		if ( attrs.length % 2 != 0 ) {
			throw new IllegalArgumentException("non-even argument lists");
		}
		for ( int i = 0 ; i < attrs.length ; i += 2)
		{
			st.setAttribute(attrs[i].toString(), attrs[i+1]);
		}
		return st ;
	}

	public static void main(String[] args)
	{
		StringTemplate st = html.getInstanceOf("test");
		st.setAttribute("abc", null);
		System.out.println(st);
	}

}
