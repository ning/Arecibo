package com.ning.arecibo.agent.datasource.jmx;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;

public class JMXClientCache {
	private static final Logger log = Logger.getLogger(JMXClientCache.class);
	
	private static final String baseHashKeyDelimiter = ":";
	private static final String fullHashKeyDelimiter = "@";
	
	private final HashMap<String,_CachedClientWrapper> jmxClientCache;
	private final AtomicLong generationCount;
	
	public JMXClientCache() {
		jmxClientCache = new HashMap<String,_CachedClientWrapper>();
		generationCount = new AtomicLong(0L);
	}
	
	public synchronized Pair<String,JMXClient> acquireClient(String host, int port)
			throws DataSourceException
	{
		
		String baseHashKey = getBaseHashKey(host, port);
		_CachedClientWrapper clientWrapper = jmxClientCache.get(baseHashKey);
		
		if(clientWrapper != null) {
			
			JMXClient jmxClient = clientWrapper.getJMXClient();
			clientWrapper.addReference();
			
			String fullHashKey = getFullHashKey(baseHashKey,clientWrapper.getGenerationCount());
			return new Pair<String,JMXClient>(fullHashKey,jmxClient);
		}
		else {
			// create a new one
			JMXClient jmxClient = connectToJMX(host, port);	
		
			clientWrapper = new _CachedClientWrapper(jmxClient,generationCount.incrementAndGet());
			clientWrapper.addReference();
			jmxClientCache.put(baseHashKey,clientWrapper);
		
			String fullHashKey = getFullHashKey(baseHashKey,clientWrapper.getGenerationCount());
			return new Pair<String,JMXClient>(fullHashKey,jmxClient);
		}
	}
	
	public synchronized void releaseClient(String fullHashKey) 
		throws DataSourceException
	{
		releaseClient(fullHashKey,false);
	}
	
	public synchronized void releaseClient(String fullHashKey,boolean notifyInvalid) 
		throws DataSourceException
	{
		Pair<String,Long> hashKeyParts = getHashKeyComponents(fullHashKey);
		String baseHashKey = hashKeyParts.getFirst();
		Long genCount = hashKeyParts.getSecond();
		
		_CachedClientWrapper clientWrapper = jmxClientCache.get(baseHashKey);
		
		if(clientWrapper == null || clientWrapper.getGenerationCount() != genCount) {
			log.info("JMXClient to release from cache is no longer valid, skipping");
			return;
		}
		
		int refCount = clientWrapper.removeReference();
		if(refCount == 0 || notifyInvalid) {
			jmxClientCache.remove(baseHashKey);
			JMXClient jmxClient = clientWrapper.getJMXClient();
				
			try {
				log.info("Closing connection to JMX host with cacheKey:  '%s'", baseHashKey);
				jmxClient.close();
			}
			catch(IOException ioEx) {
				throw new DataSourceException("IOException:",ioEx);
			}
		}
	}
	
	public synchronized JMXClient getCachedJMXClient(String hashKey) {
		_CachedClientWrapper clientWrapper = jmxClientCache.get(hashKey);
		if(clientWrapper != null)
			return clientWrapper.getJMXClient();
		else
			return null;
	}
	
	private String getBaseHashKey(String host, int port) {
		return host + baseHashKeyDelimiter + port + baseHashKeyDelimiter;
	}
	
	private String getFullHashKey(String baseHashKey,long count) {
		return baseHashKey + fullHashKeyDelimiter + count;
	}
	
	private Pair<String,Long> getHashKeyComponents(String fullHashKey) {
		
		String[] parts = fullHashKey.split(fullHashKeyDelimiter);
		Pair<String,Long> retPair = new Pair<String,Long>(parts[0],Long.parseLong(parts[1]));
		
		return retPair;
	}
	
	private JMXClient connectToJMX(String host, int port)
		throws DataSourceException
	{
		JMXClient jmxClient;
		String hostAndPort = host + ":" + port;
		
		try {
            // we no longer use the JMXClient's built in timeout facility (it can leak threads)
            // now use the system property to set the rmi connection timeout
            jmxClient = new JMXClient(hostAndPort);
			log.info("Connected to JMX host and port:  '%s'", hostAndPort);
		}
		catch (IOException e) {
			String errorStr = String.format("Could not initialize JMXClient at %s", hostAndPort);
			throw new DataSourceException(errorStr,e);
		}
		
		return jmxClient;
	}	
	
	private class _CachedClientWrapper {
		
		private final JMXClient jmxClient;
		private final AtomicInteger referenceCount;
		private final long generationCount;
		
		public _CachedClientWrapper(JMXClient jmxClient,long generationCount) {
			this.jmxClient = jmxClient;
			this.referenceCount = new AtomicInteger(0);
			this.generationCount = generationCount;
		}
		
		public JMXClient getJMXClient() {
			return this.jmxClient;
		}
		
		public int addReference() {
			return this.referenceCount.incrementAndGet();
		}
		
		public int removeReference() {
			return this.referenceCount.decrementAndGet();
		}
		
		public long getGenerationCount() {
			return this.generationCount;
		}
	}
}
