package com.ning.arecibo.aggregator.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.weakref.jmx.Managed;
import com.google.inject.Inject;
import com.ning.arecibo.aggregator.concurrent.guice.KeyedExecutorSupport;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.jmx.MonitorableManaged;

public class KeyedExecutor
{
	private static final Logger log = Logger.getLogger(KeyedExecutor.class);

	private final ConcurrentHashMap<String, KeyedBucket> buckets = new ConcurrentHashMap<String, KeyedBucket>();
	private final ExecutorService globalExecutor;
    private final AtomicInteger numScheduledBuckets;
    private final AtomicInteger numExecutingBuckets;

    @Inject
	public KeyedExecutor(@KeyedExecutorSupport ExecutorService exe)
	{
		this.globalExecutor = exe ;
        this.numScheduledBuckets = new AtomicInteger(0);
        this.numExecutingBuckets = new AtomicInteger(0);
	}

	public void execute(String key, final Runnable r)
	{
		if ( !buckets.containsKey(key) ) {
			buckets.putIfAbsent(key, new KeyedBucket(key)) ;
		}
		execute(buckets.get(key), r);
	}

	private void execute(final KeyedBucket bucket, Runnable r)
	{
		bucket.queue.add(r);
		synchronized (bucket.guard) {
			if (!bucket.guard.get()) {
				bucket.guard.set(true);
                bucket.setWaitTimeStart();
				globalExecutor.execute(new Runnable()
				{
					public void run()
					{
                        bucket.setWaitTimeEnd();
                        numExecutingBuckets.getAndIncrement();

						//log.debug("entering active thread for key %s", bucket.key );
						try {
							while (!bucket.queue.isEmpty()) {
								while (!bucket.queue.isEmpty()) {
									Runnable theRunnable = bucket.queue.take();
									//log.debug("processing for key %s", bucket.key );
									if (theRunnable != null) {
										theRunnable.run();
									}
								}
							}
						}
						catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						synchronized (bucket.guard) {
							bucket.guard.set(false);
						}
						//log.debug("leaving active thread for key %s", bucket.key);

                        numExecutingBuckets.getAndDecrement();
                        numScheduledBuckets.getAndDecrement();
					}
				});

                numScheduledBuckets.getAndIncrement();
			}
		}
	}

	private class KeyedBucket
	{
		final LinkedBlockingQueue<Runnable> queue ;
		final AtomicBoolean guard ;
        private final AtomicLong waitTimeStart;
        private final AtomicLong waitTimeEnd;
        private final String key;

		private KeyedBucket(String key)
		{
			this.guard = new AtomicBoolean();
			this.queue = new LinkedBlockingQueue<Runnable>();
			this.key = key;
            this.waitTimeStart = new AtomicLong(0L);
            this.waitTimeEnd = new AtomicLong(0L);
		}

        public void setWaitTimeStart() {
            waitTimeStart.set(System.currentTimeMillis());
            waitTimeEnd.set(0L);
        }

        public void setWaitTimeEnd() {
            waitTimeEnd.set(System.currentTimeMillis());
        }

        public long getLastWaitTime() {
            long startMs = waitTimeStart.get();
            long endMs = waitTimeEnd.get();

            if(startMs == 0L) {
                return 0L;
            }
            else if(endMs > 0L) {
                return endMs - startMs;
            }
            else {
                return System.currentTimeMillis() - startMs;
            }
        }
	}

    @Managed
    public int getNumWaitingBuckets() {
        return numScheduledBuckets.get() - numExecutingBuckets.get();
    }

    @Managed
    public int getNumExecutingBuckets() {
        return numExecutingBuckets.get();
    }

    @MonitorableManaged(monitored = true)
    public Map<String,Object> getBucketStats()
    {
        HashMap<String,Object> statsMap = new HashMap<String,Object>();

        int numBuckets = buckets.size();

        int minQueueSize = -1;
        int maxQueueSize = -1;
        int totalQueueSize = 0;
        double avgQueueSize = 0.0;
        long totalLastWaitTime = 0L;
        double avgLastWaitTime = 0.0;
        for ( KeyedBucket b : buckets.values() ) {

            int queueSize = b.queue.size();
            if(queueSize > maxQueueSize)
                maxQueueSize = queueSize;
            if(minQueueSize == -1 || queueSize < minQueueSize)
                minQueueSize = queueSize;

            totalQueueSize += queueSize;
            totalLastWaitTime += b.getLastWaitTime();
        }
        
        if(numBuckets > 0) {
            avgQueueSize = (double)totalQueueSize / (double)numBuckets;
            avgLastWaitTime = (double)totalLastWaitTime / (double)numBuckets;
        }
        else {
            minQueueSize = 0;
            maxQueueSize = 0;
        }

        statsMap.put("numBuckets", numBuckets);
        statsMap.put("numWaitingBuckets", getNumWaitingBuckets());
        statsMap.put("numExecutingBuckets", getNumExecutingBuckets());

        statsMap.put("minQueueSize", minQueueSize);
        statsMap.put("maxQueueSize", maxQueueSize);
        statsMap.put("avgQueueSize", avgQueueSize);
        statsMap.put("avgLastWaitTime", avgLastWaitTime);

        return statsMap;
    }

	@Managed
	public String[] getBucketQueueSizes()
	{
		List<String> list = new ArrayList<String>();
		for ( KeyedBucket b : buckets.values() ) {
			if ( b.guard.get() ) {
				list.add(String.format("%s %d (active)", b.key, b.queue.size()));
			}
            else {
                list.add(String.format("%s %d", b.key, b.queue.size()));
            }
		}
		return list.toArray(new String[list.size()]) ;
	}
}
