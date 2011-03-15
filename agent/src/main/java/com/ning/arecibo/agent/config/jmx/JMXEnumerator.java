package com.ning.arecibo.agent.config.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import com.ning.arecibo.agent.datasource.jmx.JMXClient;

public class JMXEnumerator
{
	private static final HashSet<String> legalTypes = new HashSet<String>();

	static {
		legalTypes.add("int");
		legalTypes.add("long");
		legalTypes.add("float");
		legalTypes.add("double");
		legalTypes.add("java.lang.Integer");
		legalTypes.add("java.lang.Long");
		legalTypes.add("java.lang.Float");
		legalTypes.add("java.lang.Double");
	}

	static Pattern[] names =
			{
					Pattern.compile(".*name=(.*)"),
					Pattern.compile(".*type=(.*)"),
					Pattern.compile(".*name=(catalog).*")
			};

	static Pattern[] filters =
			{
					Pattern.compile("ning\\.core\\.cache\\.([^,]*),.*"),
					Pattern.compile("ning\\.ipbanning\\.([^,]*),.*"),
					Pattern.compile("ning\\.messaging\\.outbound\\.smtp.([^,]*),.*"),
					Pattern.compile("(.*),context=/"),
					Pattern.compile("(.*),instance=.*")
			};

	static String[] remove = {",", "=", ";", " ", "$", "[ning-jetty-conf.xml]", "-", "\"", "/" , "."};

	final JMXClient client;

	public JMXEnumerator(String url) throws IOException
	{
		client = new JMXClient(url);
	}

	public List<MBeanAttribute> enumerate() throws IOException, ReflectionException, InstanceNotFoundException, IntrospectionException
	{
		List<MBeanAttribute> list = new ArrayList<MBeanAttribute>();
		Set<ObjectName> instances = client.getMBeanServerConnection().queryNames(null, null);
		for (ObjectName desc : new TreeSet<ObjectName>(instances)) {
			MBeanInfo info = client.getMBeanServerConnection().getMBeanInfo(desc);
			ObjectInstance obj = client.getMBeanServerConnection().getObjectInstance(desc);
			System.out.printf("%s, %s\n", desc, obj);
			String accessor = desc.toString();
			for (MBeanAttributeInfo attr : info.getAttributes()) {
				if (attr.isReadable() && legalTypes.contains(attr.getType())) {
					list.add(new MBeanAttribute(attr.getName(), resolvePrettyName(accessor), accessor));
				}
			}
		}
		return list;
	}

	public String resolvePrettyName(String name)
	{
		for (Pattern p : names) {
			Matcher m = p.matcher(name);
			if (m.matches()) {
				String matched = m.group(1);
				return clean(filter(matched));
			}
		}
		return name;
	}

	private String clean(String s)
	{
		String c = s;
		for (String r : remove) {
			c = c.replace(r, "");
		}
		return c;
	}

	private String filter(String matched)
	{
		for (Pattern p : filters) {
			Matcher m = p.matcher(matched);
			if (m.matches()) {
				String filtered = m.group(1);
				return filtered;
			}
		}
		return matched;
	}

	public static class MBeanAttribute
	{
		public final String objectName ;
		public final String eventType;
		public final String attribute;

        public MBeanAttribute(String attribute, String eventType, String objectName)
		{
			this.attribute = attribute;
			this.eventType = eventType;
			this.objectName = objectName;
		}

		public String getAttribute()
		{
			return attribute;
		}

		public String getEventType()
		{
			return eventType;
		}

		public String getObjectName()
		{
			return objectName;
		}
    }

	/*
	public static void main(String args[]) throws Exception
	{
		File outputDir = new File(new File("."), "generated_config");
		if (outputDir.exists()) {
			FileUtil.recursiveDelete(outputDir);
		}
		outputDir.mkdirs();
		List<GalaxyCoreStatus> list = GalaxyShowWrapper.getCoreStatusList(args[0], args[1]);
		for (GalaxyCoreStatus item : list) {
			if (Text.equals(item.getRunStatus(), "running")) {
				System.out.printf("surveying %s %s\n", item.getZoneHostName(), item.getCoreType());

				try {
					JMXEnumerator jenum = new JMXEnumerator(item.getZoneHostName() + ":8989");
					List<MBeanAttribute> config = jenum.enumerate();

					if ( config.size() > 0 ) {
						File f = new File(outputDir, item.getCoreType() + "_full_monitoring.config");
						if (!f.exists()) {
							FileOutputStream out = new FileOutputStream(f);
							PrintWriter pw = new PrintWriter(out);
							for ( MBeanAttribute a : config ) {
								pw.printf("%s; %s; %s; %s\n", a.getObjectName(), a.getAttributeName(), a.getDisplayName(), a.getAttributeName());
							}
							pw.flush();
							out.flush();
							out.close();
						}
					}
				}
				catch(Exception	e)
				{
					System.out.printf("unable to poke %s\n", item.getZoneHostName());
				}
			}
		}
	}
	*/
}
