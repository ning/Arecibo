package com.ning.arecibo.agent.datasource.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.jmx.JMXConfig;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;
import com.ning.arecibo.agent.datasource.jmx.JMXClient.MBeanDescriptor;
import com.ning.arecibo.util.Logger;

public class JMXOperationInvocationDataSource extends JMXDataSource
{
	private static final Logger log = Logger.getLogger(JMXOperationInvocationDataSource.class);

	private static final String operationInvocationDelimiter = "invoke:";

	private volatile List<String> operationNameList = null;
	private volatile List<String[]> argumentsList = null;
	private volatile List<String[]> signatureList = null;

	public JMXOperationInvocationDataSource(Config config, JMXClientCache jmxClientCache, JMXParserManager jmxParserManager) throws DataSourceException
	{
		super(config, jmxClientCache, jmxParserManager);
	}

	@Override
	public synchronized void initialize()
		throws DataSourceException
	{
		super.initialize();
		this.operationNameList = null;
	}


	@Override
	public synchronized void finalizePreparation()
	{
		if (operationNameList == null) {
			operationNameList = new ArrayList<String>();
			argumentsList = new ArrayList<String[]>();
			signatureList = new ArrayList<String[]>();

			for (String attribute : attributes) {

				// parse operation and argument list from config attribute
				Pattern pattern = Pattern.compile(operationInvocationDelimiter + "(\\S*)\\((\\S*)\\)\\[(\\S*)\\]");
				Matcher matcher = pattern.matcher(attribute);
				if (matcher.matches()) {
					String operationName = matcher.group(1);

					String argumentGroup = matcher.group(2);
					String[] arguments = argumentGroup.split(",");

					String signatureGroup = matcher.group(3);
					String[] signature = signatureGroup.split(",");

					operationNameList.add(operationName);
					argumentsList.add(arguments);
					signatureList.add(signature);
				}
			}
		}
	}

	@Override
	public synchronized Map<String, Object> getValues()
		throws DataSourceException
	{
		try {
			MBeanDescriptor mbeanDesc = monitoredMBean.getMBeanDescriptor();
			MBeanServerConnection mbeanConn = jmxClient.getMBeanServerConnection();
	
			Map<String, Object> values = new HashMap<String, Object>();
	
			Iterator<String> attributeIter = attributes.iterator();
			Iterator<String> operationIter = operationNameList.iterator();
			Iterator<String[]> argumentsIter = argumentsList.iterator();
			Iterator<String[]> signatureIter = signatureList.iterator();

			while (attributeIter.hasNext() && operationIter.hasNext() && argumentsIter.hasNext() && signatureIter.hasNext()) {

				Object result = mbeanConn.invoke(mbeanDesc.getObjectName(), operationIter.next(), argumentsIter.next(), signatureIter.next());

				values.put(configHashKeyMap.get(attributeIter.next()), result);
			}

			return values;
		}
		catch (IOException ioEx) {
			log.info("Unable to get values for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out as a DataSourceException
			throw new DataSourceException("IOException:",ioEx);
		}
		catch (JMException jmEx) {
			log.info("Unable to get values for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out as a DataSourceException
			throw new DataSourceException("JMException:",jmEx);
		}
		catch(RuntimeException ruEx) {
			log.info("Unable to get values for %s", this.objectName);
			closeAndInvalidateClient();
			
			// throw this out as a DataSourceException
			throw new DataSourceException("RuntimeException:",ruEx);
		}	
	}
	
	private static boolean matchesAttributeType(String attribute) {
		if(attribute.contains(operationInvocationDelimiter))
			return true;
		
		return false;
	}	
	
	public static boolean matchesConfig(Config config) {

        if (!(config instanceof JMXConfig))
            return false;

        return matchesAccessorType(((JMXConfig)config).getObjectName()) && matchesAttributeType(((JMXConfig)config).getAttribute());
	}

	@Override
	public DataSourceType getDataSourceType() {
		return DataSourceType.JMXOperationInvocation;
	}

}
