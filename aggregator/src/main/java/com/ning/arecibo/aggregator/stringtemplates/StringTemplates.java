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
