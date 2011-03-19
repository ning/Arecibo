package com.ning.arecibo.collector.dao;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.arecibo.collector.RemoteCollector;
import com.ning.arecibo.collector.ResolutionTagGenerator;
import com.ning.arecibo.collector.ResolutionUtils;
import com.ning.arecibo.collector.config.CollectorConfig;
import com.ning.arecibo.collector.contentstore.DbEntryUtil;
import com.ning.arecibo.collector.guice.CollectorConstants;
import com.ning.arecibo.event.MapEvent;
import com.ning.arecibo.event.MonitoringEvent;
import com.ning.arecibo.eventlogger.Event;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.esper.MiniEsperEngine;
import com.ning.arecibo.util.jmx.MonitorableManaged;
import com.ning.arecibo.util.jmx.MonitoringType;
import com.ning.arecibo.util.xml.XStreamUtils;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang.StringUtils;
import org.skife.jdbi.v2.Folder;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.PreparedBatchPart;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerMapper;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;

public class CollectorDAO extends UnicastRemoteObject implements RemoteCollector
{
	private final static Logger log = Logger.getLogger(CollectorDAO.class);

    private final static String RETRY_LABEL_PREFIX = "_r";
    private final static int NUM_SWAP_BUFFERS = 2;

	private final XStream xstream = XStreamUtils.getXStreamNoStringCache() ;

	private final IDBI dbi;
	private final String tablespace;
	private final int tablespaceStatsUpdateIntervalMinutes;
	private final int maxTableSpaceMB;
	private final int maxSplitAndSweepInitialDelayMinutes;
	private final int throttlePctFree;
	private final long hostUpdateInterval;
	private final Boolean readOnlyMode;
	private final int bufferWindowInSecs;
	private final int[] reductionFactors;
	private final Map<String,EventTableDescriptor> eventTableDescriptors;
	private final ResolutionUtils resolutionUtils;
    private final int maxTriageThreads;
    private final int maxBatchInsertThreads;
    private final int minBatchInsertSize;
    private final int maxBatchInsertSize;
    private final int maxAsynchTriageQueueSize;
    private final int maxAsynchInsertQueueSize;
    private final int maxPendingEvents;
    private final long maxPendingEventsCheckIntervalMs;
    private final Boolean enableBatchRetryOnIntegrityViolation;
    private final Boolean enableDuplicateEventLogging;
    private final Boolean enablePerTableInserts;
    private final Boolean enablePreparedBatchInserts;
    private final AtomicLong retryIdCounter = new AtomicLong(0L);
    private volatile boolean maxPendingEventsExceeded = false;

	private static final TimeZone localTZ = TimeZone.getDefault();
	private static final long gmtOffset = localTZ.getOffset(System.currentTimeMillis());
	private final MiniEsperEngine<Long> transactionTimeStats;
    private final MiniEsperEngine<Double> insertTimePerEventStats;
	private final AtomicReference<TablespaceStats> tablespaceStats = new AtomicReference<TablespaceStats>();
	private final AtomicLong eventsInserted = new AtomicLong(0L);
	private final AtomicLong discardedEventsDueToSpace = new AtomicLong(0L);
	private final AtomicLong discardedEventsDueToConsecutiveDuplicate = new AtomicLong(0L);
    private final AtomicLong discardedEventsDueToTriageQueueOverflow = new AtomicLong(0L);
    private final AtomicLong discardedEventsDueToInsertQueueOverflow = new AtomicLong(0L);
    private final AtomicLong discardedEventsDueToIntegrityViolation = new AtomicLong(0L);
    private final AtomicLong retriedEventsDueToIntegrityViolation = new AtomicLong(0L);
    private final AtomicLong retriedEventsDueToIntegrityViolationInserted = new AtomicLong(0L);
    private final AtomicLong asynchTriageQueueEventCount = new AtomicLong(0L);
    private final AtomicLong asynchInsertQueueEventCount = new AtomicLong(0L);

	// Last Event Caches
	private final ConcurrentHashMap<String,Map<String,MapEvent>> lastHostEvents = new ConcurrentHashMap<String,Map<String,MapEvent>>();
	private final ConcurrentHashMap<String,Map<String,MapEvent>> lastTypeEvents = new ConcurrentHashMap<String,Map<String,MapEvent>>();
	private final ConcurrentHashMap<String,Map<String,MapEvent>> lastPathEvents = new ConcurrentHashMap<String,Map<String,MapEvent>>();

	// Metadata caches
	private final ConcurrentHashMap<String,CachedHost> hostMap = new ConcurrentHashMap<String,CachedHost>();
	private final ConcurrentHashMap<String,Integer> typeMap = new ConcurrentHashMap<String,Integer>();
	private final ConcurrentHashMap<String,Integer> pathMap = new ConcurrentHashMap<String,Integer>();
	private final ConcurrentHashMap<String,Integer> eventTypeMap = new ConcurrentHashMap<String,Integer>();

	// Insert Queues
    private final AtomicInteger bufferIndex = new AtomicInteger(0);
    private final BlockingQueue<Event>[] bufferQueue;
    private final ReentrantReadWriteLock[] bufferQueueLock;

    // Triage Queue
    private final ArrayBlockingQueue<Runnable> asynchTriageQueue;
    private final ThreadPoolExecutor asynchTriageExecutor;

    // Insert Queue
    private final ArrayBlockingQueue<Runnable> asynchInsertQueue;
    private final ThreadPoolExecutor asynchInsertExecutor;

	private final Random random = new Random(System.currentTimeMillis());

    // general thread scheduler
    private final ScheduledExecutorService generalThreadScheduler = Executors.newScheduledThreadPool(10);
    private static final String STATEMENTS_LOCATION = "com/ning/arecibo/collector/dao/sql";


    @Inject
	public CollectorDAO(@Named(CollectorConstants.COLLECTOR_DB) IDBI dbi,
                        CollectorConfig config,
	                    Registry registry,
	                    ResolutionUtils resolutionUtils) throws RemoteException, AlreadyBoundException
	{
        log.info("LocalTimeZone = %s", localTZ);

        if(config.isCollectorReadOnlyMode()) {
            log.info("Initializing CollectorDAO in ReadOnlyMode");
        }

		this.dbi = dbi;
		this.tablespace = config.getTableSpaceName();
        this.tablespaceStatsUpdateIntervalMinutes = config.getTablespaceStatsUpdateIntervalMinutes();
		this.maxTableSpaceMB = config.getMaxTableSpaceMB();
		this.maxSplitAndSweepInitialDelayMinutes = config.getMaxSplitAndSweepInitialDelayMinutes();
		this.throttlePctFree = config.getThrottlePctFreeThreshold();
        this.hostUpdateInterval = config.getHostUpdateInterval();
		this.readOnlyMode = config.isCollectorReadOnlyMode();
        this.enableBatchRetryOnIntegrityViolation = config.getEnableBatchRetryOnIntegrityViolation();
        this.enableDuplicateEventLogging = config.getEnableDuplicateEventLogging();
        this.enablePerTableInserts = config.getEnablePerTableInserts();
        this.enablePreparedBatchInserts = config.getEnablePreparedBatchInserts();
		this.bufferWindowInSecs = config.getBufferWindowInSeconds();
		this.reductionFactors = getReductionFactors(config.getReductionFactorList());

        String numPartitionsToKeepList = config.getNumPartitionsToKeepList();
        String splitIntervalInMinutesList = config.getSplitIntervalInMinutesList();
        String numPartitionsToSplitAheadList = config.getNumPartitionsToSplitAheadList();

        this.resolutionUtils = resolutionUtils;
        this.eventTableDescriptors = getEventTableDescriptors(resolutionUtils,
            config.getReductionFactorList(),
            numPartitionsToKeepList,
            splitIntervalInMinutesList,
            numPartitionsToSplitAheadList);

        this.maxTriageThreads = config.getMaxTriageThreads();
        this.maxBatchInsertThreads = config.getMaxBatchInsertThreads();
        this.minBatchInsertSize = config.getMinBatchInsertSize();
        this.maxBatchInsertSize = config.getMaxBatchInsertSize();
        this.maxAsynchTriageQueueSize = config.getMaxAsynchTriageQueueSize();
        this.maxAsynchInsertQueueSize = config.getMaxAsynchInsertQueueSize();
        this.maxPendingEvents = config.getMaxPendingEvents();
        this.maxPendingEventsCheckIntervalMs = config.getMaxPendingEventsCheckIntervalMs();

        // set up statistics window engine for transaction/insert time
		this.transactionTimeStats = new MiniEsperEngine<Long>("TransactionTime", Long.class);
        this.insertTimePerEventStats = new MiniEsperEngine<Double>("InsertTimePerEvent", Double.class);


        // define the asynch triage queue
        this.asynchTriageQueue = new ArrayBlockingQueue<Runnable>(this.maxAsynchTriageQueueSize,true);

        // set up asynchTriageExecutor, provide a RejectedExecutionHandler to throw out batches when overflow reached
        this.asynchTriageExecutor = initializeAsynchTriageExecutor();

        // define the asynch insert queue
        this.asynchInsertQueue = new ArrayBlockingQueue<Runnable>(this.maxAsynchInsertQueueSize,true);

        // set up asynchInsertExecutor, provide a RejectedExecutionHandler to throw out batches when overflow reached
        this.asynchInsertExecutor = initializeAsynchInsertExecutor();


        // init buffer queues
        bufferQueue = new LinkedBlockingQueue[NUM_SWAP_BUFFERS];
        bufferQueueLock = new ReentrantReadWriteLock[NUM_SWAP_BUFFERS];
        for(int i=0;i<NUM_SWAP_BUFFERS;i++) {
            bufferQueue[i] = new LinkedBlockingQueue<Event>();
            bufferQueueLock[i] = new ReentrantReadWriteLock();
        }

        // set up buffer swapping schedule, for receiving incoming data
        scheduleBufferSwapThread();

        // set up tablespace stats collection
        scheduleTablespaceStatsThread();

        // set up split and sweep schedule
        scheduleSplitAndSweepThread();

        // set up maxPendingEvents check schedule
        scheduleMaxPendingEventsChecker();


        // seed caches with initial values
        try {
            populateCaches();
        }
        catch (CollectorDAOException cdEx) {
            // make sure we log this, and continue, db might recover later
            log.warn(cdEx,"CollectorDAOException:");
        }

        // bind to the rmi registry
        registry.bind(RemoteCollector.class.getSimpleName(), this);
	}

    public void insertBuffered(final List<Event> events) {
        //log.debug("adding %d events to buffer queue", events.size());

        int currIndex = bufferIndex.get();
        try {
            // lock the buffer before adding data to it
            // (use readLock since multiple threads can do this)
            bufferQueueLock[currIndex].readLock().lock();
            bufferQueue[currIndex].addAll(events);
        }
        finally{
            bufferQueueLock[currIndex].readLock().unlock();
        }
    }

    public void insertBuffered(final Event event) {
        //log.debug("adding 1 events to buffer queue");
        int currIndex = bufferIndex.get();
        try {
            // lock the buffer before adding data to it
            // (use readLock since multiple threads can do this)
            bufferQueueLock[currIndex].readLock().lock();
            bufferQueue[currIndex].add(event);
        }
        finally{
            bufferQueueLock[currIndex].readLock().unlock();
        }
    }

    public boolean getOkToProcessEvents() {
        return !maxPendingEventsExceeded;
    }

    private ThreadPoolExecutor initializeAsynchTriageExecutor() {

        // set up asynchTriageExecutor, provide a RejectedExecutionHandler to throw out batches when overflow reached
        return new  ThreadPoolExecutor(this.maxTriageThreads,
                                        this.maxTriageThreads,
                                        60L, TimeUnit.SECONDS,
                                        this.asynchTriageQueue,
                                        new NamedThreadFactory(getClass().getSimpleName() + "_asynchTriageExecutor"),
                                        new RejectedExecutionHandler()
                                        {
                                            @Override
                                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                                AsynchTriageRunnable atRemoved = (AsynchTriageRunnable) r;
                                                int numRemoved = atRemoved.size();

                                                log.info("Removing batch from asynch triage queue due to max backlog (%d) reached: %d events being thrown away",
                                                        CollectorDAO.this.maxAsynchTriageQueueSize, numRemoved);

                                                discardedEventsDueToTriageQueueOverflow.getAndAdd(numRemoved);
                                                asynchTriageQueueEventCount.getAndAdd(-numRemoved);
                                            }
                                        }
                                );
    }

    private ThreadPoolExecutor initializeAsynchInsertExecutor() {

        // set up asynchInsertExecutor, provide a RejectedExecutionHandler to throw out batches when overflow reached
        return new ThreadPoolExecutor(this.maxBatchInsertThreads,
                                        this.maxBatchInsertThreads,
                                        60L, TimeUnit.SECONDS,
                                        this.asynchInsertQueue,
                                        new NamedThreadFactory(getClass().getSimpleName() + "_asynchInsertExecutor"),
                                        new RejectedExecutionHandler()
                                        {
                                            @Override
                                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                                AsynchInsertRunnable aiRemoved = (AsynchInsertRunnable) r;
                                                int numRemoved = aiRemoved.size();

                                                log.info("Removing batch from asynch insert queue due to max backlog (%d) reached: %d events being thrown away",
                                                        CollectorDAO.this.maxAsynchInsertQueueSize, numRemoved);

                                                discardedEventsDueToInsertQueueOverflow.getAndAdd(numRemoved);
                                                asynchInsertQueueEventCount.getAndAdd(-numRemoved);
                                            }
                                        }
                                );
    }

    private void scheduleBufferSwapThread() {

        // set up buffer swapping scheduler, for receiving incoming data
        this.generalThreadScheduler.scheduleWithFixedDelay(new Runnable() {

                    public void run()
                    {
                        try {

                            // update the swap buffer
                            int currIndex = bufferIndex.get();
                            int newIndex = (currIndex + 1) % NUM_SWAP_BUFFERS;
                            bufferIndex.set(newIndex);

                            final List<Event> list = new ArrayList<Event>();
                            try {
                                // lock this buffer to drain it
                                // (use writeLock since only 1 thread can do this, make sure to wait for all in process inserts first)
                                bufferQueueLock[currIndex].writeLock().lock();
                                bufferQueue[currIndex].drainTo(list);
                            }
                            finally {
                                bufferQueueLock[currIndex].writeLock().unlock();
                            }

                            if (!list.isEmpty()) {
                                AsynchTriageRunnable atRunnable = new AsynchTriageRunnable(list);
                                atRunnable.submit();
                            }
                        }
                        catch (RuntimeException e) {
                            log.warn(e, "RuntimeException:");
                        }
                    }
                }, bufferWindowInSecs, bufferWindowInSecs, TimeUnit.SECONDS);
    }

    private void scheduleTablespaceStatsThread() {
        // set up tablespace stats collection
        this.generalThreadScheduler.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                try {
                    updateTablespaceStats();
                }
                catch(CollectorDAOException e) {
                    log.warn(e,"CollectorDAOException:");
                }
            }
        }, 0, this.tablespaceStatsUpdateIntervalMinutes, TimeUnit.MINUTES);
    }


    private void scheduleSplitAndSweepThread() {

        for(EventTableDescriptor tableDescriptor:this.eventTableDescriptors.values()) {

            if (tableDescriptor.getSplitIntervalInMinutes() < 0)
                continue;

            int initialDelayMinutes = random.nextInt(this.maxSplitAndSweepInitialDelayMinutes);
            log.info("Scheduling splitAndSweep thread in " + initialDelayMinutes + " minutes for tableDescriptor: " + tableDescriptor.getHashKey());

            final EventTableDescriptor finalTableDescriptor = tableDescriptor;

            this.generalThreadScheduler.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    try {
                        splitAndSweep(finalTableDescriptor);
                    }
                    catch (CollectorDAOException cdEx) {
                        log.warn(cdEx, "Got exception during splitAndSweep, tableDescriptor: " + finalTableDescriptor.getHashKey());
                    }
                }
            }, random.nextInt(this.maxSplitAndSweepInitialDelayMinutes), tableDescriptor.getSplitIntervalInMinutes(), TimeUnit.MINUTES);
        }
    }

    private void scheduleMaxPendingEventsChecker() {
        this.generalThreadScheduler.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                boolean currValue = maxPendingEventsExceeded;

                long currPendingEvents = asynchTriageQueueEventCount.get() + asynchInsertQueueEventCount.get();
                maxPendingEventsExceeded = currPendingEvents > maxPendingEvents;

                if(currValue != maxPendingEventsExceeded) {
                    if(maxPendingEventsExceeded) {
                        log.warn("Max pending events exceeded (%d > %d), blocking event submission",currPendingEvents,maxPendingEvents);
                    }
                    else {
                        log.info("Max pending events no longer exceeded (%d < %d), re-enabling event submission",currPendingEvents,maxPendingEvents);
                    }
                }
            }

        },maxPendingEventsCheckIntervalMs,maxPendingEventsCheckIntervalMs,TimeUnit.MILLISECONDS);
    }

	private void populateCaches() throws CollectorDAOException
	{
		try {
			dbi.withHandle(new HandleCallback<Object>()
			{
				public Object withHandle(Handle handle) throws Exception
				{
					handle.createQuery(STATEMENTS_LOCATION + ":getHosts")
							.fold(hostMap, new Folder<ConcurrentHashMap<String, CachedHost>>()
							{
								public ConcurrentHashMap<String, CachedHost> fold(ConcurrentHashMap<String, CachedHost> map, ResultSet rs) throws SQLException
								{
									CachedHost cachedHost = new CachedHost(rs);
									if ( cachedHost.getHost() != null ) {
										map.putIfAbsent(cachedHost.getHost(), cachedHost);
									}
									return map;
								}
							});
					return null;
				}
			});

			dbi.withHandle(new HandleCallback<Object>()
			{
				public Object withHandle(Handle handle) throws Exception
				{
					handle.createQuery(STATEMENTS_LOCATION + ":getTypes")
							.fold(typeMap, new Folder<ConcurrentHashMap<String, Integer>>()
							{
								public ConcurrentHashMap<String, Integer> fold(ConcurrentHashMap<String, Integer> map, ResultSet rs) throws SQLException
								{
									String t = rs.getString("dep_type") ;
									int id = rs.getInt("id") ;
									if ( t != null ) {
										map.putIfAbsent(t, id);
									}
									return map ;
								}
							});
					return null;
				}
			});

			dbi.withHandle(new HandleCallback<Object>()
			{
				public Object withHandle(Handle handle) throws Exception
				{
					handle.createQuery(STATEMENTS_LOCATION + ":getPaths")
							.fold(pathMap, new Folder<ConcurrentHashMap<String, Integer>>()
							{
								public ConcurrentHashMap<String, Integer> fold(ConcurrentHashMap<String, Integer> map, ResultSet rs) throws SQLException
								{
									String p = rs.getString("dep_path") ;
									if ( p != null ) {
										map.putIfAbsent(p, rs.getInt("id"));
									}
									return map ;
								}
							});
					return null;
				}
			});

			dbi.withHandle(new HandleCallback<Object>()
			{
				public Object withHandle(Handle handle) throws Exception
				{
					handle.createQuery(STATEMENTS_LOCATION + ":getEventTypes")
							.fold(eventTypeMap, new Folder<ConcurrentHashMap<String, Integer>>()
							{
								public ConcurrentHashMap<String, Integer> fold(ConcurrentHashMap<String, Integer> map, ResultSet rs) throws SQLException
								{
									String et = rs.getString("event_type");
									if ( et != null ) {
										map.putIfAbsent(et, rs.getInt("event_type_id"));
									}
									return map ;
								}
							});
					return null;
				}
			});
		}
		catch(RuntimeException ruEx) {
			throw new CollectorDAOException("RuntimeException:",ruEx);
		}

	}

	private void splitAndSweep(final EventTableDescriptor tableDescriptor) throws CollectorDAOException
	{
	    if(readOnlyMode) {
            AggregationType aggType = tableDescriptor.getAggregationType();
            String baseTableName = aggType.getBaseTableName();
            String tableName = baseTableName + resolutionUtils.getResolutionTag(tableDescriptor.getReductionFactor());
            log.info("skipping split and sweep table %s (readOnlyMode)", tableName);
	        return;
        }

	    try {
    		dbi.withHandle(new HandleCallback<Void>(){
    			public Void withHandle(Handle handle) throws Exception
    			{
                    setMaxTs(tableDescriptor,0L);
                    final Timestamp ts = new Timestamp(tableDescriptor.getMaxTs() - gmtOffset + TimeUnit.MILLISECONDS.convert(
                    		tableDescriptor.getSplitIntervalInMinutes() * tableDescriptor.getSplitNumPartitionsAhead(), TimeUnit.MINUTES));

    				return handle.inTransaction(new TransactionCallback<Void>()
    				{
    					public Void inTransaction(Handle handle, TransactionStatus transactionStatus) throws Exception
    					{
    						AggregationType aggType = tableDescriptor.getAggregationType();
							String baseTableName = aggType.getBaseTableName();
							String tableName = baseTableName + resolutionUtils.getResolutionTag(tableDescriptor.getReductionFactor());

                            log.info("split and sweep table %s at %s", tableName, ts);
                            handle.createStatement(STATEMENTS_LOCATION + ":split_and_sweep")
								.bind("table_name", tableName)
								.bind("ts", ts )
								.bind("keep", tableDescriptor.getNumPartitionsToKeep())
                                .execute();

                            return null;
    					}
    				});
    			}
    		});

    		log.info("split and sweep completed");
	    }
	    catch (RuntimeException e) {
        	throw new CollectorDAOException("RuntimeException:", e);
    	}
	}

	private void updateTablespaceStats() throws CollectorDAOException
	{
	    try {
    		// update table space stats
    		TablespaceStats newStats = dbi.withHandle(new HandleCallback<TablespaceStats>()
    		{
    			public TablespaceStats withHandle(Handle handle) throws Exception
    			{
				return handle.createQuery(STATEMENTS_LOCATION + ":table_space_stats")
    						.bind("tablespace", tablespace)
    						.map(new ResultSetMapper<TablespaceStats>()
    						{
    							public TablespaceStats map(int i, ResultSet rs, StatementContext statementContext) throws SQLException
    							{
    								return new TablespaceStats(rs.getLong("freeAllocatedMB"), rs.getLong("totalAllocatedMB"), rs.getLong("usedMB"), maxTableSpaceMB);
    							}
    						})
    						.first();
    			}
    		});

    		tablespaceStats.set(newStats);
	    }
	    catch (RuntimeException e) {
        	throw new CollectorDAOException("RuntimeException:", e);
    	}
	}


	private boolean checkTablespaceStats() {

    	TablespaceStats stats = tablespaceStats.get();
        if ( stats != null && stats.pctFree < throttlePctFree ) {
			log.warn("tablespace %.1f percent free, discarding events", stats.pctFree);
			return false;
		}
        return true;
	}

    private void insert(final String label,final List<Event> events) throws CollectorDAOException
    {
        if(!checkTablespaceStats()) {
        	discardedEventsDueToSpace.addAndGet(events.size());
        	return;
        }

        // sort events chronologically, to combat against duplicate events
        Collections.sort(events,new EventTimestampComparator());

		long start = System.currentTimeMillis();
	    try {
            dbi.inTransaction(new TransactionCallback<Object>()
            {
                public Object inTransaction(Handle handle, TransactionStatus transactionStatus) throws Exception
                {
                    if(enablePreparedBatchInserts) {
                        insertListWithPreparedBatches(label,events,handle);
                    }
                    else {
                        insertBatch(label,events,handle);
                    }
                    return null;
                }
            });

            eventsInserted.addAndGet(events.size());
            updateLastEventCaches(events);

            if(label.contains(RETRY_LABEL_PREFIX)) {
                retriedEventsDueToIntegrityViolationInserted.addAndGet(events.size());
            }
	    }
        catch (RuntimeException e) {

            // find bottom message in exception stack
            Throwable t = e;
            while(t.getCause() != null)
                t = t.getCause();

            if(t instanceof SQLException) {

                SQLException sqlEx = (SQLException)t;

                // NOTE: this is Oracle specific, error code 1 is for integrity violations
                if(sqlEx.getErrorCode() != 1) {
                    throw new CollectorDAOException("Got RuntimeException:",e);
                }

                String subLabel;
                if(!log.isDebugEnabled() && label.contains(RETRY_LABEL_PREFIX))
                    subLabel = label.substring(0,label.indexOf(RETRY_LABEL_PREFIX)) + " (retried)";
                else
                    subLabel = label;

                log.info("failed batch insert for %s: %s",subLabel,sqlEx.getMessage().trim());

                if(!enableBatchRetryOnIntegrityViolation || events.size() <= 1) {
                    log.info("throwing out %d record(s) for %s",events.size(),subLabel);

                    if(enableDuplicateEventLogging && events.size() == 1) {
                        Event event = events.get(0);
                        log.info("throwing out duplicate event: %s %s %s", event.getEventType(),event.getTimestamp(),events.toString());
                    }

                    discardedEventsDueToIntegrityViolation.addAndGet(events.size());
                }
                else {
                    // NOTE: BatchUpdateException.getUpdateCounts() doesn't appear to work
                    // correctly with the oracle driver, in this context, so can't be clever
                    // about detecting which events to retry, etc.

                    retriedEventsDueToIntegrityViolation.addAndGet(events.size());

                    List<Event> subList1;
                    List<Event> subList2;

                    // split the batch in half, try to zero in on the bad record(s)
                    subList1 = events.subList(0,events.size()/2);
                    subList2 = events.subList(events.size()/2,events.size());

                    String retryId1 = label + RETRY_LABEL_PREFIX + retryIdCounter.getAndIncrement();
                    String retryId2 = label + RETRY_LABEL_PREFIX + retryIdCounter.getAndIncrement();

                    if(log.isDebugEnabled()) {
                        log.debug("retrying subdivided lists of size %d (%s) and %d (%s)",subList1.size(),retryId1,subList2.size(),retryId2);
                    }
                    else {
                        log.info("retrying subdivided lists of size %d and %d (%s)",subList1.size(),subList2.size(),subLabel);
                    }


                    AsynchInsertRunnable aiRunnable1 = new AsynchInsertRunnable(retryId1,subList1);
                    AsynchInsertRunnable aiRunnable2 = new AsynchInsertRunnable(retryId2,subList2);

                    aiRunnable1.submit();
                    aiRunnable2.submit();
                }
            }
            else {
                throw new CollectorDAOException("Got RuntimeException:",e);
            }
        }
		finally {
			long time = System.currentTimeMillis() - start ;
			transactionTimeStats.send(time);

            if(events.size() > 0)
                insertTimePerEventStats.send((double)time / (double)events.size());
		}
    }

    private void insertListWithPreparedBatches(String label,List<Event> events,Handle handle) throws CollectorDAOException
    {
        // TODO: Much of this is specific to the aggregator plugin, need to remove this dependency somehow

        // Note: If the asynchTriageExecutor is in use, the code here that disambiguates the aggType/templateName should
        // be the same for every event in the loop, so might be able to save efficiency by moving that logic outside of the
        // for loop, or even pass in directly as part of the triaged info for the list.  For now, keep the full disambiguation
        // on the event level, in case we want to restore the ability to send heterogeneous event lists in a batch

        try {

            Map<String,PreparedBatch> preparedBatches = new HashMap<String,PreparedBatch>();

            for(Event event : events) {
                Integer eventTypeID = getCachedEventTypeID(event.getEventType());

                Timestamp ts = new Timestamp(event.getTimestamp() - gmtOffset);

                PreparedBatchPart preparedBatchPart = null;

                if (event instanceof MapEvent) {

                    MapEvent mapEvent = (MapEvent) event;
                    MonitoringEvent me = new MonitoringEvent(mapEvent);

                    Object aggObj = mapEvent.getValue("aggType");
                    if (aggObj == null) {
                        log.warn("Couldn't determine aggType");
                        continue;
                    }

                    AggregationType aggType = AggregationType.valueOf(aggObj.toString().toUpperCase());

                    String resolutionTag = mapEvent.getValue("reduction").toString();

                    // update the maxTs for this table
                    EventTableDescriptor tableDescriptor = this.eventTableDescriptors.get(EventTableDescriptor.getHashKey(aggType, resolutionTag));
                    if (tableDescriptor == null) {
                        throw new CollectorDAOException("Could not find eventTableDescriptor for aggType = '" +
                                aggType + "', resolutionFactor = '" + resolutionTag + "'");
                    }
                    tableDescriptor.setMaxTs(event.getTimestamp());

                    String templateName = aggType.getTemplateName();
                    String tableName;
                    if (resolutionTag != null && resolutionTag.length() > 0) {
                        tableName = aggType.getBaseTableName() + resolutionTag;
                    }
                    else {
                        tableName = aggType.getBaseTableName();
                    }

                    PreparedBatch preparedBatch = null;
                    if(!readOnlyMode) {
                        preparedBatch = preparedBatches.get(templateName);
                        if(preparedBatch == null) {
                            preparedBatch = handle.prepareBatch(templateName);
                            preparedBatches.put(templateName,preparedBatch);
                        }
                    }

                    switch (aggType) {
                        case HOST:
                            if (checkNotEqualLastValue(lastHostEvents, me.getHostName() + resolutionTag, mapEvent) && !readOnlyMode) {
                                preparedBatchPart = preparedBatch.add();
                                preparedBatchPart.define("host_event_table", tableName)
                                                    .bind("ts", ts)
                                                    .bind("event_type_id", eventTypeID)
                                                    .bind("host_id", getCachedHost(me).id);
                            }
                            break;
                        case TYPE:
                            if (checkNotEqualLastValue(lastTypeEvents, me.getDeployedType() + resolutionTag, mapEvent) && !readOnlyMode) {
                                preparedBatchPart = preparedBatch.add();
                                preparedBatchPart.define("type_event_table", tableName)
                                                    .bind("ts", ts)
                                                    .bind("event_type_id", eventTypeID)
                                                    .bind("type_id", getCachedTypeID(me.getDeployedType()));
                            }
                            break;
                        case PATH:
                            if (checkNotEqualLastValue(lastPathEvents, me.getDeployedType() + ":" + me.getDeployedConfigSubPath() + resolutionTag, mapEvent) && !readOnlyMode) {
                                preparedBatchPart = preparedBatch.add();
                                preparedBatchPart.define("path_event_table", tableName)
                                                    .bind("ts", ts)
                                                    .bind("event_type_id", eventTypeID)
                                                    .bind("type_id", getCachedTypeID(me.getDeployedType()))
                                                    .bind("path_id", getCachedPathID(me.getDeployedConfigSubPath()));
                            }
                            break;
                    }
                }
                else if (!readOnlyMode) {

                    // insert in the generic table (shouldn't happen currently)
                    PreparedBatch preparedBatch = preparedBatches.get("generic");
                    if(preparedBatch == null) {
                        preparedBatch = handle.prepareBatch(AggregationType.GENERIC.getTemplateName());
                        preparedBatches.put("generic",preparedBatch);
                    }

                    preparedBatchPart = preparedBatch.add();
                    preparedBatchPart.define("generic_event_table", "generic_events")
                                        .bind("ts", ts)
                                        .bind("event_type_id", eventTypeID);

                }
                if (preparedBatchPart != null) {
                    DbEntryUtil.bind(preparedBatchPart, xstream.toXML(event));
                }
                else {
                    if (readOnlyMode) {
                        //log.debug("event not inserted, read_only_mode : %s", e);
                    }
                    else {
                        if(enableDuplicateEventLogging) {
                            log.info("ignoring duplicate event: %s %s %s", event.getEventType(),event.getTimestamp(),events.toString());
                        }
                        discardedEventsDueToConsecutiveDuplicate.incrementAndGet();
                    }
                }
            }

            for(String templateName:preparedBatches.keySet()) {
                PreparedBatch preparedBatch = preparedBatches.get(templateName);

                int batchSize = preparedBatch.getSize();
                int[] results = preparedBatch.execute();

                int count = 0;
                for(int result:results) {
                    if(result > 0)
                        count += result;
                    else if(result == Statement.SUCCESS_NO_INFO)
                        count += 1;
                }
                if(count != batchSize) {
                    // this doesn't seem to happen, instead a BatchUpdateException gets thrown in this case
                    log.debug("failed to insert %d out of %d records (%s)",batchSize-count,batchSize,templateName);
                }
                else {
                    log.debug("inserted %d events (%s)",count,label);
                }
            }
        }
        catch(RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException: ", ruEx);
        }
    }

    private void insertBatch(String label,List<Event> events, Handle handle) throws CollectorDAOException {
        // TODO: Much of this is specific to the aggregator plugin, need to remove this dependency somehow

        try {
            for(Event e:events) {
                final Integer eventTypeID = getCachedEventTypeID(e.getEventType());

                final Timestamp ts = new Timestamp(e.getTimestamp() - gmtOffset);

                Update updateStmt = null;

                if (e instanceof MapEvent) {

                    MapEvent event = (MapEvent) e;
                    final MonitoringEvent me = new MonitoringEvent(event);

                    Object aggObj = event.getValue("aggType");
                    if (aggObj == null) {
                        log.warn("Couldn't determine aggType");
                        return;
                    }

                    AggregationType aggType = AggregationType.valueOf(aggObj.toString().toUpperCase());

                    String resolutionTag = event.getValue("reduction").toString();

                    // update the maxTs for this table
                    EventTableDescriptor tableDescriptor = this.eventTableDescriptors.get(EventTableDescriptor.getHashKey(aggType, resolutionTag));
                    if (tableDescriptor == null) {
                        throw new CollectorDAOException("Could not find eventTableDescriptor for aggType = '" +
                                aggType + "', resolutionFactor = '" + resolutionTag + "'");
                    }
                    tableDescriptor.setMaxTs(e.getTimestamp());

                    String templateName = aggType.getTemplateName();
                    String tableName;
                    if (resolutionTag != null && resolutionTag.length() > 0) {
                        tableName = aggType.getBaseTableName() + resolutionTag;
                    }
                    else {
                        tableName = aggType.getBaseTableName();
                    }

                    switch (aggType) {
                        case HOST:
                            if (checkNotEqualLastValue(lastHostEvents, me.getHostName() + resolutionTag, event) && !readOnlyMode) {
                                updateStmt = handle.createStatement(templateName)
                                        .define("host_event_table", tableName)
                                        .bind("ts", ts)
                                        .bind("event_type_id", eventTypeID)
                                        .bind("host_id", getCachedHost(me).id);
                            }
                            break;
                        case TYPE:
                            if (checkNotEqualLastValue(lastTypeEvents, me.getDeployedType() + resolutionTag, event) && !readOnlyMode) {
                                updateStmt = handle.createStatement(templateName)
                                        .define("type_event_table", tableName)
                                        .bind("ts", ts)
                                        .bind("event_type_id", eventTypeID)
                                        .bind("type_id", getCachedTypeID(me.getDeployedType()));
                            }
                            break;
                        case PATH:
                            if (checkNotEqualLastValue(lastPathEvents, me.getDeployedType() + ":" + me.getDeployedConfigSubPath() + resolutionTag, event) && !readOnlyMode) {
                                updateStmt = handle.createStatement(templateName)
                                        .define("path_event_table", tableName)
                                        .bind("ts", ts)
                                        .bind("event_type_id", eventTypeID)
                                        .bind("type_id", getCachedTypeID(me.getDeployedType()))
                                        .bind("path_id", getCachedPathID(me.getDeployedConfigSubPath()));
                            }
                            break;
                    }
                }
                else if (!readOnlyMode) {
                    updateStmt = handle.createStatement(STATEMENTS_LOCATION + ":insert_generic_event")
                            .define("generic_event_table", "generic_events")
                            .bind("ts", ts)
                            .bind("event_type_id", eventTypeID);
                }
                if (updateStmt != null) {
                    DbEntryUtil.bindAndExecute(updateStmt, xstream.toXML(e));
                }
                else {
                    if (readOnlyMode) {
                        //log.debug("event not inserted, read_only_mode : %s", e);
                    }
                    else {
                        if(enableDuplicateEventLogging) {
                            log.info("ignoring duplicate event: %s %s %s", e.getEventType(),e.getTimestamp(),e.toString());
                        }
                        discardedEventsDueToConsecutiveDuplicate.incrementAndGet();
                    }
                }
            }
        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException: ", ruEx);
        }
    }


    private void updateLastEventCaches(List<Event> events) throws CollectorDAOException {

        try {
            for (Event event : events) {
                if (event instanceof MapEvent) {

                    MapEvent mapEvent = (MapEvent) event;
                    MonitoringEvent me = new MonitoringEvent(mapEvent);

                    Object aggObj = mapEvent.getValue("aggType");
                    if (aggObj == null) {
                        log.warn("Couldn't determine aggType");
                        return;
                    }

                    AggregationType aggType = AggregationType.valueOf(aggObj.toString().toUpperCase());
                    String resolutionTag = mapEvent.getValue("reduction").toString();

                    // TODO: Assumes for base level reduction, resolutionTag = ""
                    // need to make more explicit with parameterization for referencing last*Events maps, by passing a resolutionTag explicitly

                    switch (aggType) {
                        case HOST:
                            saveLastValue(lastHostEvents, me.getHostName() + resolutionTag, mapEvent);
                            break;
                        case TYPE:
                            saveLastValue(lastTypeEvents, me.getDeployedType() + resolutionTag, mapEvent);
                            break;
                        case PATH:
                            saveLastValue(lastPathEvents, me.getDeployedType() + ":" + me.getDeployedConfigSubPath() + resolutionTag, mapEvent);
                            break;
                    }
                }
            }
        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException: ", ruEx);
        }
    }

	private Integer getCachedTypeID(final String type)
			throws CollectorDAOException
	{
		try {

			if (!typeMap.containsKey(type)) {

				Integer saved = null;
				saved = dbi.withHandle(new HandleCallback<Integer>()
				{
					public Integer withHandle(Handle handle) throws Exception
					{
						return handle.createQuery(STATEMENTS_LOCATION + ":getTypeByName")
								.bind("dep_type", type)
								.map(IntegerMapper.FIRST).first();
					}
				});

				if (saved == null) {
                    final Integer next = getNextID();
                    Integer old = typeMap.putIfAbsent(type, next);
                    if (old == null && !readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(STATEMENTS_LOCATION + ":insertType")
                                    .bind("dep_type", type)
                                    .bind("id", next)
                                    .execute();
                                return null;
                            }
                        });
                    }
				}
				else {
					typeMap.putIfAbsent(type, saved);
				}
			}
			return typeMap.get(type);

		}
		catch(RuntimeException ruEx) {
			throw new CollectorDAOException("RuntimeException:",ruEx);
		}
	}

	private Integer getCachedPathID(final String path)
			throws CollectorDAOException
	{
		try {

			if (!pathMap.containsKey(path)) {

				Integer saved = null;
				saved = dbi.withHandle(new HandleCallback<Integer>()
				{
					public Integer withHandle(Handle handle) throws Exception
					{
						return handle.createQuery(STATEMENTS_LOCATION + ":getPathByName")
								.bind("dep_path", path)
								.map(IntegerMapper.FIRST).first();
					}
				});

				if (saved == null) {
                    final Integer next = getNextID();
                    Integer old = pathMap.putIfAbsent(path, next);
                    if (old == null && !readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(STATEMENTS_LOCATION + ":insertPath")
                                        .bind("dep_path", path)
                                        .bind("id", next)
                                        .execute();
                                return null;
                            }
                        });
                    }
				}
				else {
					pathMap.putIfAbsent(path, saved);
				}
			}
			return pathMap.get(path);

		}
		catch(RuntimeException ruEx) {
			throw new CollectorDAOException("RuntimeException:",ruEx);
		}
	}

	private Integer getCachedEventTypeID(final String eventType)
			throws CollectorDAOException
	{
		try {

			if (!eventTypeMap.containsKey(eventType)) {

				Integer saved = null;
				saved = dbi.withHandle(new HandleCallback<Integer>()
				{
					public Integer withHandle(Handle handle) throws Exception
					{
						return handle.createQuery(STATEMENTS_LOCATION + ":getEventTypeByName")
								.bind("event_type", eventType)
								.map(IntegerMapper.FIRST).first();
					}
				});

				if (saved == null) {
                    final Integer next = getNextID();
                    Integer old = eventTypeMap.putIfAbsent(eventType, next);
                    if (old == null && !readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(STATEMENTS_LOCATION + ":insertEventType")
                                        .bind("event_type", eventType)
                                        .bind("event_type_id", next)
                                        .execute();
                                return null;
                            }
                        });
                    }
				}
				else {
					eventTypeMap.putIfAbsent(eventType, saved);
				}
			}
			return eventTypeMap.get(eventType);
		}
		catch(RuntimeException ruEx) {
			throw new CollectorDAOException("RuntimeException:",ruEx);
		}
	}

    private CachedHost getCachedHost(final MapEvent mapEvent)
            throws CollectorDAOException {

        CachedHost cachedHost = new CachedHost(mapEvent);
        return getCachedHost(cachedHost,(String)mapEvent.getValue(MonitoringEvent.KEY_HOST));
    }

	private CachedHost getCachedHost(final MonitoringEvent me)
			throws CollectorDAOException {

		CachedHost cachedHost = new CachedHost(me);
        return getCachedHost(cachedHost,me.getHostName());
    }

    private CachedHost getCachedHost(CachedHost cachedHost,final String hostName)
            throws CollectorDAOException {

        try {

			if (!hostMap.containsKey(hostName)) {

				CachedHost saved = null;
				saved = dbi.withHandle(new HandleCallback<CachedHost>()
				{
					public CachedHost withHandle(Handle handle) throws Exception
					{
						return handle.createQuery(STATEMENTS_LOCATION + ":getHostByName")
								.bind("host", hostName)
								.map(new ResultSetMapper<CachedHost>()
								{
									public CachedHost map(int i, ResultSet rs, StatementContext statementContext) throws SQLException
									{
										return new CachedHost(rs);
									}
								}).first();
					}
				});

				if (saved == null) {
                    CachedHost old = hostMap.putIfAbsent(hostName, cachedHost);
                    if (old == null) {
                        cachedHost.insert();
                        return cachedHost;
                    }
                    else {
                        return old ;
                    }
				}
				else {
					hostMap.putIfAbsent(hostName, saved);
					return saved ;
				}
			}
			else {
				CachedHost cached = hostMap.get(hostName);
				cached.update(cachedHost);
				return cached ;
			}
		}
		catch(RuntimeException ruEx) {
			throw new CollectorDAOException("RuntimeException:",ruEx);
		}
	}

    private Integer getNextID()
            throws CollectorDAOException {
        try {
            return dbi.withHandle(new HandleCallback<Integer>() {
                public Integer withHandle(Handle handle) throws Exception {
                    return handle.createQuery(STATEMENTS_LOCATION + ":nextId")
                            .map(IntegerMapper.FIRST)
                            .first();
                }
            });
        }
        catch (RuntimeException ruEx) {
            throw new CollectorDAOException("RuntimeException:", ruEx);
        }
    }

	private void setMaxTs(EventTableDescriptor tableDescriptor,long ts)
	{
		tableDescriptor.setMaxTs(Math.max(Math.max(tableDescriptor.getMaxTs(), ts), System.currentTimeMillis()));
	}

    private Map<String, MapEvent> getLastValuesSince(long since,Map<String,MapEvent> lastEvents) {
        return getLastValuesSince(since,lastEvents,null);
    }

    private Map<String, MapEvent> getLastValuesSince(long since,Map<String,MapEvent> lastEvents,String eventType) {
        if(eventType == null) {
            if (since == 0L)
                return lastEvents;
            else {
                Map<String, MapEvent>lastEventsSince = new HashMap<String, MapEvent>();

                for (String key : lastEvents.keySet()) {
                    MapEvent mapEvt = lastEvents.get(key);
                    if (mapEvt.getTimestamp() >= since) {
                        lastEventsSince.put(key, mapEvt);
                    }
                }

                return lastEventsSince;
            }
        }
        else {
            Map<String, MapEvent>lastEventsSince = new HashMap<String, MapEvent>();

            for (String key : lastEvents.keySet()) {
                MapEvent mapEvt = lastEvents.get(key);
                if (since == 0L || mapEvt.getTimestamp() >= since) {
                    if(mapEvt.getEventType().equals(eventType))
                        lastEventsSince.put(key, mapEvt);
                }
            }

            return lastEventsSince;
        }
    }

    private List<String> getLastEventTypesSince(long since,Map<String,MapEvent> lastEvents) {
        List<String> lastEventTypes = new ArrayList<String>();

        for (String key : lastEvents.keySet()) {
            MapEvent mapEvt = lastEvents.get(key);
            if (since == 0L || mapEvt.getTimestamp() >= since) {
                lastEventTypes.add(mapEvt.getEventType());
            }
        }

        return lastEventTypes;
    }

    private void saveLastValue(ConcurrentHashMap<String, Map<String, MapEvent>> map, String key, MapEvent event) {
        if (!map.containsKey(key)) {
            map.putIfAbsent(key, new ConcurrentHashMap<String, MapEvent>());
        }
        Map<String, MapEvent> perKeyMap = map.get(key);
        perKeyMap.put(event.getEventType(), event);
    }

    private void saveLastValue(Map<String, MapEvent> map, MapEvent event) {
        map.put(event.getEventType(), event);
    }

    private boolean checkNotEqualLastValue(ConcurrentHashMap<String, Map<String, MapEvent>> map, String key, MapEvent event) {
        if (!map.containsKey(key)) {
            return true;
        }
        Map<String, MapEvent> perKeyMap = map.get(key);

        return checkNotEqualLastValue(perKeyMap,event);
    }

    private boolean checkNotEqualLastValue(Map<String, MapEvent> map, MapEvent event) {
        MapEvent old = map.get(event.getEventType());

        // return false if this looks the same as the most recent
        return old == null || old.getTimestamp() != event.getTimestamp();
    }

    @Override
    public int[] getReductionFactors() {
        return this.reductionFactors;
    }

    @Override
    public ResolutionTagGenerator getResolutionTagGenerator() {
        return resolutionUtils;
    }


    @Override
    public Map<String, MapEvent> getLastValuesForHost(long since,String host) throws RemoteException
    {
        return getLastValuesForHost(since,host,null);
    }

    @Override
	public Map<String, MapEvent> getLastValuesForHost(long since,String host,String eventType) throws RemoteException
	{
        // gets base reduction events (e.g. with reductionTag = "")
		Map<String, MapEvent> map = lastHostEvents.get(host);
		if ( map != null ) {
            return getLastValuesSince(since,map,eventType);
		}
		else {
			log.warn("last values for host %s not found", host);
			return null;
		}
	}



    @Override
    public Map<String, MapEvent> getLastValuesForType(long since,String type) throws RemoteException
    {
        return getLastValuesForType(since,type,null);
    }

    @Override
	public Map<String, MapEvent> getLastValuesForType(long since,String type,String eventType) throws RemoteException
	{
        // gets base reduction events (e.g. with reductionTag = "")
		Map<String, MapEvent> map = lastTypeEvents.get(type);
		if ( map != null ) {
            return getLastValuesSince(since,map,eventType);
		}
		else {
			log.warn("last values for type %s not found", type);
			return null;
		}
	}



    @Override
    public Map<String, MapEvent> getLastValuesForPathWithType(long since,String path, String type) throws RemoteException
    {
        return getLastValuesForPathWithType(since,path,type,null);
    }

    @Override
	public Map<String, MapEvent> getLastValuesForPathWithType(long since,String path, String type,String eventType) throws RemoteException
	{
        // gets base reduction events (e.g. with reductionTag = "")
		Map<String, MapEvent> map = lastPathEvents.get(type + ":" + path);
		if ( map != null ) {
            return getLastValuesSince(since,map,eventType);
		}
		else {
			log.warn("last values for path %s type %s not found", path, type);
			return null;
		}
	}



    @Override
    public List<String> getLastEventTypesForHost(long since,String host) throws RemoteException
    {
        // gets base reduction events (e.g. with reductionTag = "")
        Map<String, MapEvent> map = lastHostEvents.get(host);
        if ( map != null ) {
            return getLastEventTypesSince(since,map);
        }
        else {
            log.warn("last values for host %s not found", host);
            return null;
        }
    }

    @Override
    public List<String> getLastEventTypesForType(long since,String type) throws RemoteException
    {
        // gets base reduction events (e.g. with reductionTag = "")
        Map<String, MapEvent> map = lastTypeEvents.get(type);
        if ( map != null ) {
            return getLastEventTypesSince(since,map);
        }
        else {
            log.warn("last values for type %s not found", type);
            return null;
        }
    }

    @Override
    public List<String> getLastEventTypesForPathWithType(long since,String path, String type) throws RemoteException
    {
        // gets base reduction events (e.g. with reductionTag = "")
        Map<String, MapEvent> map = lastPathEvents.get(type + ":" + path);
        if ( map != null ) {
            return getLastEventTypesSince(since,map);
        }
        else {
            log.warn("last values for path %s type %s not found", path, type);
            return null;
        }
    }

    @Override
	public Collection<String> getHosts() throws RemoteException
	{
        HashSet<String> hosts = new HashSet<String>();
        for (CachedHost cachedHost : hostMap.values()) {
            if(cachedHost.host != null)
                hosts.add(cachedHost.host);
        }
        return hosts;
	}

    @Override
	public Collection<String> getHosts(long since) throws RemoteException
	{
		HashSet<String> hosts = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.host != null &&
                    cachedHost.last_refreshed >= since) {
				hosts.add(cachedHost.host);
			}
		}
		return hosts;
	}

    @Override
	public Collection<String> getHosts(long since, String type) throws RemoteException
	{
		HashSet<String> hosts = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.host != null &&
                    cachedHost.last_refreshed >= since &&
					StringUtils.equals(cachedHost.type, type)) {
				hosts.add(cachedHost.host);
			}
		}
		return hosts;
	}

    @Override
	public Collection<String> getHosts(long since, String type, String path) throws RemoteException
	{
		HashSet<String> hosts = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.host != null &&
                    cachedHost.last_refreshed >= since &&
					StringUtils.equals(cachedHost.type, type) &&
					StringUtils.equals(cachedHost.path, path)) {
				hosts.add(cachedHost.host);
			}
		}
		return hosts;
	}

    @Override
	public Collection<String> getTypes() throws RemoteException
	{
		HashSet<String> types = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
            if(cachedHost.type != null)
			    types.add(cachedHost.type);
		}
		return types;
	}

    @Override
	public Collection<String> getTypes(long since) throws RemoteException
	{
		HashSet<String> types = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.type != null && cachedHost.last_refreshed >= since) {
				types.add(cachedHost.type);
			}
		}
		return types;
	}

    @Override
	public Collection<String> getTypes(long since, String path) throws RemoteException
	{
		HashSet<String> types = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.type != null &&
                    cachedHost.last_refreshed >= since &&
					StringUtils.equals(cachedHost.path, path)) {
				types.add(cachedHost.type);
			}
		}
		return types;
	}

    @Override
	public Collection<String> getPaths() throws RemoteException
	{
		HashSet<String> paths = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
            if(cachedHost.path != null)
			    paths.add(cachedHost.path);
		}
		return paths;
	}

    @Override
	public Collection<String> getPaths(long since) throws RemoteException
	{
		HashSet<String> paths = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.path != null && cachedHost.last_refreshed >= since) {
				paths.add(cachedHost.path);
			}
		}
		return paths;
	}

    @Override
	public Collection<String> getPaths(long since, String type) throws RemoteException
	{
		HashSet<String> paths = new HashSet<String>();
		for (CachedHost cachedHost : hostMap.values()) {
			if (cachedHost.path != null &&
                    cachedHost.last_refreshed >= since &&
					StringUtils.equals(cachedHost.type, type)) {
				paths.add(cachedHost.path);
			}
		}
		return paths;
	}

	// Managed MBeans
	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
	public long getTransactionCount()
	{
		return transactionTimeStats.getCount();
	}

	@MonitorableManaged(monitored = true)
	public double getAvgTransactionTimeInMs()
	{
		return transactionTimeStats.getAverage();
	}

	@MonitorableManaged(monitored = true)
    public double getAvgInsertTimePerEventInMs()
    {
        return insertTimePerEventStats.getAverage();
    }

	@MonitorableManaged(monitored = true)
	public long getTableSpaceFreeAllocatedMB()
	{
		TablespaceStats stats = tablespaceStats.get();
		if (stats == null) {
			return -1;
		}
		return stats.freeAllocatedMB;
	}

	@MonitorableManaged(monitored = true)
	public double getTableSpacePctFree()
	{
		TablespaceStats stats = tablespaceStats.get();
		if (stats == null) {
			return -1;
		}
		return stats.pctFree;
	}

	@MonitorableManaged(monitored = true)
	public long getTableSpaceUsedMB()
	{
		TablespaceStats stats = tablespaceStats.get();
		if (stats == null) {
			return -1;
		}
		return stats.usedMB;
	}

	@MonitorableManaged(monitored = true)
	public long getTableSpaceTotalAllocatedMB()
	{
		TablespaceStats stats = tablespaceStats.get();
		if (stats == null) {
			return -1;
		}
		return stats.totalAllocatedMB;
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getEventsInserted() {
        return this.eventsInserted.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getRetriedEventsDueToIntegrityViolationInserted() {
        return this.retriedEventsDueToIntegrityViolationInserted.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
	public long getRetriedEventsDueToIntegrityViolation() {
		return this.retriedEventsDueToIntegrityViolation.get();
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
	public long getDiscardedEventsDueToSpace() {
		return this.discardedEventsDueToSpace.get();
	}

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getDiscardedEventsDueToConsecutiveDuplicate() {
        return this.discardedEventsDueToConsecutiveDuplicate.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getDiscardedEventsDueToTriageQueueOverflow() {
        return this.discardedEventsDueToTriageQueueOverflow.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getDiscardedEventsDueToInsertQueueOverflow() {
        return this.discardedEventsDueToInsertQueueOverflow.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.RATE, MonitoringType.COUNTER })
    public long getDiscardedEventsDueToIntegrityViolation() {
        return this.discardedEventsDueToIntegrityViolation.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE })
    public long getAsynchTriageQueueSize() {
        return this.asynchTriageQueue.size();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE })
    public long getAsynchInsertQueueSize() {
        return this.asynchInsertQueue.size();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE })
    public long getAsynchInsertQueuedEventCount() {
        return this.asynchInsertQueueEventCount.get();
    }

	@MonitorableManaged(monitored = true, monitoringType = { MonitoringType.VALUE })
	public long getAsynchTriageQueuedEventCount() {
		return this.asynchTriageQueueEventCount.get();
	}

    private static class TablespaceStats
    {
        final long freeAllocatedMB, totalAllocatedMB, usedMB, maxMB;
        final double pctFree;

        private TablespaceStats(long freeAllocatedMB, long totalAllocatedMB, long usedMB, long maxMB)
        {
            this.freeAllocatedMB = freeAllocatedMB;
            this.totalAllocatedMB = totalAllocatedMB;
            this.usedMB = usedMB;
            this.maxMB = maxMB;

            if(this.maxMB > 0) {
                this.pctFree = 100.0*(double)(maxMB - usedMB)/(double)maxMB;
            }
            else {
                this.pctFree = 100.0*(double)freeAllocatedMB / (double)totalAllocatedMB;
            }
        }
    }

    private class CachedHost
    {
        private final String host;
        private volatile String path, type, env, ver;
        private volatile Integer id ;

        private volatile long last_refreshed = 0L;
        private volatile long last_updated = 0L;

        private CachedHost(MapEvent mapEv)
        {
            this.last_refreshed = mapEv.getTimestamp();
            this.host = (String)mapEv.getValue(MonitoringEvent.KEY_HOST);
            this.path = (String)mapEv.getValue(MonitoringEvent.KEY_CONFIG_PATH);
            this.type = (String)mapEv.getValue(MonitoringEvent.KEY_TYPE);
        }

        private CachedHost(MonitoringEvent me)
        {
            this.last_refreshed = me.getTimestamp();
            this.host = me.getHostName();
            this.path = me.getDeployedConfigSubPath();
            this.type = me.getDeployedType();
        }

        public CachedHost(ResultSet rs) throws SQLException
        {
            this.last_updated = rs.getTimestamp("updated_dt").getTime();
            this.host = rs.getString("host");
            this.path = rs.getString("dep_path");
            this.type = rs.getString("dep_type");
            this.id = rs.getInt("id");
        }

        public void insert()
                throws CollectorDAOException
        {
            try {
                if(readOnlyMode)
                    return;

                id = getNextID();
                dbi.withHandle(new HandleCallback<Object>()
                {
                    public Object withHandle(Handle handle) throws Exception
                    {
                        handle.createStatement(STATEMENTS_LOCATION + ":insertHost")
                                .bind("id", id)
                                .bind("host", getHost())
                                .bind("dep_type", getType())
                                .bind("dep_path", getPath())
                                .execute();
                        return null;
                    }
                });
            }
            catch(RuntimeException ruEx) {
                throw new CollectorDAOException("RuntimeException:",ruEx);
            }
        }

        public void update(CachedHost other)
                throws CollectorDAOException
        {
            try {
                if (System.currentTimeMillis() - last_updated > hostUpdateInterval && checksum() != other.checksum()) {
                    log.debug("updating host info %s", other.toString());
                    this.ver = other.ver ;
                    this.path = other.path ;
                    this.type = other.type ;
                    this.env = other.env ;

                    if(!readOnlyMode) {
                        dbi.withHandle(new HandleCallback<Object>()
                        {
                            public Object withHandle(Handle handle) throws Exception
                            {
                                handle.createStatement(STATEMENTS_LOCATION + ":updateHost")
                                        .bind("id", id)
                                        .bind("host", getHost())
                                        .bind("dep_type", getType())
                                        .bind("dep_path", getPath())
                                        .execute();
                                return null;
                            }
                        });
                    }
                    this.last_updated = System.currentTimeMillis();
                }
                this.last_refreshed = other.last_refreshed;
            }
            catch(RuntimeException ruEx) {
                throw new CollectorDAOException("RuntimeException:",ruEx);
            }
        }

        public String getHost() {
            return host;
        }

        public String getPath() {
            return path;
        }

        public String getType() {
            return type;
        }

        public String getEnv() {
            return env;
        }

        public Integer getId() {
            return id;
        }

        public String getVer() {
            return ver;
        }

        public long checksum()
        {
            CRC32 crc = new CRC32();
            if ( env != null ) {
                crc.update(env.getBytes());
            }
            if ( type != null ) {
                crc.update(type.getBytes());
            }
            if ( path != null ) {
                crc.update(path.getBytes());
            }
            if ( ver != null ) {
                crc.update(ver.getBytes());
            }
            return crc.getValue();
        }

        public String toString()
        {
            return new StringBuffer().append("CachedHost[")
                    .append(id).append(",")
                    .append(host).append(",")
                    .append(type).append(",")
                    .append(path).append(",")
                    .append(last_refreshed).append(",")
                    .append("]").toString();

        }
    }

    private class AsynchTriageRunnable implements Runnable {

        private final List<Event> events;

        public AsynchTriageRunnable(List<Event> events) {
            this.events = events;
        }

        public void submit() {
            asynchTriageQueueEventCount.getAndAdd(this.size());
            asynchTriageExecutor.execute(this);
        }

        public int size() {
            if (events != null)
                return events.size();
            else
                return 0;
        }

        public void run() {

            asynchTriageQueueEventCount.getAndAdd(-this.size());
            log.info("triaging %d events in batch...",events.size());

            Map<String,List<Event>> perTableListMap = new HashMap<String,List<Event>>();

            for(Event event:events) {

                String tableName;
                if (event instanceof MapEvent) {

                    MapEvent mapEvent = (MapEvent) event;

                    Object aggObj = mapEvent.getValue("aggType");
                    if (aggObj == null) {
                        log.warn("Couldn't determine aggType");
                        continue;
                    }

                    AggregationType aggType = AggregationType.valueOf(aggObj.toString().toUpperCase());

                    if(enablePerTableInserts) {
                        String resolutionTag = mapEvent.getValue("reduction").toString();
                        if (resolutionTag != null && resolutionTag.length() > 0) {
                            tableName = aggType.getBaseTableName() + resolutionTag;
                        }
                        else {
                            tableName = aggType.getBaseTableName();
                        }
                    }
                    else {
                        tableName = "multiple tables";
                    }

                    // call these getters, which initialize db cache entries as needed,
                    // want this done outside of main batch insert transactions
                    try {
                        getCachedEventTypeID(event.getEventType());

                        switch (aggType) {
                            case HOST:
                                getCachedHost(mapEvent);
                                break;
                            case TYPE:
                                getCachedTypeID((String)mapEvent.getValue(MonitoringEvent.KEY_TYPE));
                                break;
                            case PATH:
                                getCachedTypeID((String)mapEvent.getValue(MonitoringEvent.KEY_TYPE));
                                getCachedPathID((String)mapEvent.getValue(MonitoringEvent.KEY_CONFIG_PATH));
                                break;
                        }
                    }
                    catch(CollectorDAOException cdEx) {
                        log.warn(cdEx,"CollectorDAOException:");
                        continue;
                    }
                }
                else {
                    tableName = "generic";
                }

                List<Event> perTableList = perTableListMap.get(tableName);

                if(perTableList == null) {
                    perTableList = new ArrayList<Event>();
                    perTableListMap.put(tableName,perTableList);
                }

                perTableList.add(event);
                if(perTableList.size() >= maxBatchInsertSize) {
                    submitInsertRunnable(perTableListMap.remove(tableName),tableName);
                }
            }

            // insert separate lists, organized by destination tables
            // keep a combined list for those tables which have less than the minimum size
            List<Event> combinedList = null;
            String combinedListLabel = null;
            for(String tableName:perTableListMap.keySet()) {
                List<Event> perTableList = perTableListMap.get(tableName);

                if(perTableList.size() >= minBatchInsertSize) {
                    submitInsertRunnable(perTableList,tableName);
                }
                else {
                    if(combinedList == null) {
                        combinedList = perTableList;
                        combinedListLabel = tableName;
                    }
                    else {
                        combinedList.addAll(perTableList);
                        combinedListLabel += " " + tableName;
                    }
                }
            }
            if(combinedList != null)
                submitInsertRunnable(combinedList,combinedListLabel);
        }

        private void submitInsertRunnable(List<Event> perTableList,String label) {
            AsynchInsertRunnable aiRunnable = new AsynchInsertRunnable(label,perTableList);
            aiRunnable.submit();
        }
    }

    private class AsynchInsertRunnable implements Runnable {

        private final String label;
        private final List<Event> events;

        public AsynchInsertRunnable(String label,List<Event> events) {
            this.label = label;
            this.events = events;
        }

        public void submit() {
            asynchInsertQueueEventCount.getAndAdd(this.size());
            asynchInsertExecutor.execute(this);
        }

        public int size() {
            if (events != null)
                return events.size();
            else
                return 0;
        }

        public void run() {
            try {
                asynchInsertQueueEventCount.getAndAdd(-this.size());

                String subLabel;
                if (!log.isDebugEnabled() && label.contains(RETRY_LABEL_PREFIX))
                    subLabel = label.substring(0, label.indexOf(RETRY_LABEL_PREFIX)) + " (retried)";
                else
                    subLabel = label;
                log.info("inserting %d events for %s", events.size(),subLabel);

                insert(label,events);
            }
            catch (CollectorDAOException e) {
                log.warn(e, "CollectorDAOException:");
            }
            catch (RuntimeException e) {
                log.warn(e, "RuntimeException:");
            }
        }
    }

    private class EventTimestampComparator implements Comparator<Event> {

        public int compare(Event e1,Event e2) {

            Long t1;
            Long t2;

            if(e1 instanceof MapEvent) {
                t1 = ((MapEvent)e1).getTimestamp();
            }
            else {
                t1 = 0L;
            }

            if(e2 instanceof MapEvent) {
                t2 = ((MapEvent)e1).getTimestamp();
            }
            else {
                t2 = 0L;
            }

            return t1.compareTo(t2);
        }
    }

    private int[] getReductionFactors(String reductionFactorList)
    {
        StringTokenizer reductionFactorST = new StringTokenizer(reductionFactorList, ",");
        int count = reductionFactorST.countTokens();

        int[] reductionFactors = new int[count];

        int i = 0;
        while (reductionFactorST.hasMoreTokens()) {
            // note, if this has a number format exception, it will throw a Runtimer out of here (which is ok)
            reductionFactors[i++] = Integer.parseInt(reductionFactorST.nextToken());
        }

        return reductionFactors;
    }

    private Map<String, EventTableDescriptor> getEventTableDescriptors(ResolutionUtils resUtils,
                                                                       String reductionFactorList,
                                                                       String numPartitionsToKeepList,
                                                                       String splitIntervalInMinutesList,
                                                                       String numPartitionsToSplitAheadList)
    {

        StringTokenizer reductionFactors = new StringTokenizer(reductionFactorList, ",");
        StringTokenizer numPartitionsToKeep = new StringTokenizer(numPartitionsToKeepList, ",");
        StringTokenizer splitIntervalInMinutes = new StringTokenizer(splitIntervalInMinutesList, ",");
        StringTokenizer numPartitionsToSplitAhead = new StringTokenizer(numPartitionsToSplitAheadList, ",");

        // get the size of the largest list
        int numDescriptorCombos = Math.max(reductionFactors.countTokens(),
            Math.max(numPartitionsToKeep.countTokens(),
                Math.max(splitIntervalInMinutes.countTokens(),
                    numPartitionsToSplitAhead.countTokens())));

        // parse lists in order, and ones that run out of items will just replicate the last one in the list
        // TODO: cleanup this config ugliness!
        HashMap<String, EventTableDescriptor> tableDescriptors = new HashMap<String, EventTableDescriptor>();

        int currReductionFactor = 0;
        int currNumPartitionsToKeep = 0;
        int currSplitIntervalInMinutes = 0;
        int currNumPartitionsToSplitAhead = 0;

        for (int i = 0; i < numDescriptorCombos; i++) {

            // note, if any of these have a number format exception, will throw a Runtimer out of here (which is ok)
            if (reductionFactors.hasMoreTokens()) {
                currReductionFactor = Integer.parseInt(reductionFactors.nextToken());
            }
            if (numPartitionsToKeep.hasMoreTokens()) {
                currNumPartitionsToKeep = Integer.parseInt(numPartitionsToKeep.nextToken());
            }
            if (splitIntervalInMinutes.hasMoreTokens()) {
                currSplitIntervalInMinutes = Integer.parseInt(splitIntervalInMinutes.nextToken());
            }
            if (numPartitionsToSplitAhead.hasMoreTokens()) {
                currNumPartitionsToSplitAhead = Integer.parseInt(numPartitionsToSplitAhead.nextToken());
            }

            for (AggregationType aggType : AggregationType.values()) {

                if (i > 0 && !aggType.getSupportsMultiRes()) {
                    continue;
                }

                EventTableDescriptor tableDescriptor = new EventTableDescriptor(aggType,
                    currReductionFactor,
                    resUtils.getResolutionTag(currReductionFactor),
                    currNumPartitionsToKeep,
                    currSplitIntervalInMinutes,
                    currNumPartitionsToSplitAhead);

                String hashKey = tableDescriptor.getHashKey();
                tableDescriptors.put(hashKey, tableDescriptor);
            }
        }

        return tableDescriptors;

    }

}
