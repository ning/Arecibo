package com.ning.arecibo.event.publisher;

import java.io.File;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.Pair;
import com.ning.arecibo.util.xml.XStreamUtils;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.thoughtworks.xstream.XStream;

public class BDBQueue<T>
{
    protected static final int MAX_INDEX = Integer.MAX_VALUE;
    public static final int MAX_NB_ELEMENTS = MAX_INDEX / 2;

    private int nbLoopKey;

    private static final int KEY_NOT_FOUND = -1;
    private static final long SPIN_SLEEP = 10; // sleep 10 ms

    public static Logger log = Logger.getLoggerViaExpensiveMagic();

    private static Environment dbEnvironment = null;
    private static AtomicInteger nbEnvironmentConsumers = new AtomicInteger(0);

    private Database mainDb;
    private TupleBinding tupleBinding;
    private EntryBinding keyBinding;

    private final int initialFreeKey;
    private final int maxElements;

    private SortedSet<Integer> skippedKeys;

    private AtomicInteger nextFreeKey;
    private AtomicInteger nextGetKey;
    private Semaphore permits;

    private enum EdgeType {
        EDGE_MIN,
        EDGE_MAX,
    };

    public BDBQueue(File storage, String dbName) {
        this(storage, dbName, MAX_NB_ELEMENTS);
    }

    public BDBQueue(File storage, String dbName, int maxElements) {

        if (maxElements > MAX_NB_ELEMENTS) {
            throw new RuntimeException("The queue accepts a mamximum number of " + MAX_NB_ELEMENTS);
        }
        this.maxElements = maxElements;

        initEnvironment(storage);
        nbEnvironmentConsumers.incrementAndGet();
        mainDb = openBDB(dbName);
        tupleBinding = new TBindingXML<T>();
        keyBinding = TupleBinding.getPrimitiveBinding(Integer.class);
        initialFreeKey = initializeKeys();
        skippedKeys = new TreeSet<Integer>();
    }

    public void stop() {
        closeBDBs(mainDb);
    }

    public boolean put(final T it) {
        return putWithKey(getNextFreeKey(), it);
    }

    public Pair<Integer, T> get() throws InterruptedException {

        permits.acquire();
        Pair<Integer, T> result = null;
        int key = getNextGetKey();
        do {
            result = get(key);
            if (result != null) {
                break;
            }

            //
            // Check first of there was a hole in the key space when we booted. If so
            // consume an additional permit since we counted that hole
            // If not, check if we recently missed that key because of an exception
            // or because of a OperationStatus != SUCCESS when inserting.
            //
            // At least, spin lock until the preemption has been resolved.
            //
            boolean isSkipped = isKeySkipped(key);
            if (isSkipped) {
                permits.acquire();
                if (log.isDebugEnabled()) {
                    log.debug("Skipped initial key = " + key + " tid=" + Thread.currentThread().getId() +
                            ", permits = " + permits.availablePermits());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Skipped failed key = " + key + " tid=" + Thread.currentThread().getId() +
                            ", permits = " + permits.availablePermits());
                }
                synchronized(skippedKeys) {
                    isSkipped = skippedKeys.remove(new Integer(key));
                }
            }
            //
            // If this is skipped for fetch the next entry (using same permit)
            // If this is not skipped this means two threads inserted entries
            // out of order so we spin lock until the first thread finishes.
            //
            if (isSkipped) {
                key = getNextGetKey();
            } else {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Thread " + Thread.currentThread().getId() +
                                    " sleeping for key " + key + ", permits = " + permits.availablePermits());
                    }
                    Thread.sleep(SPIN_SLEEP);
                } catch (InterruptedException ie) {
                    log.warn("Thread " + Thread.currentThread().getId() + " got interrupted");
                    Thread.interrupted();
                    break;
                }
            }
        } while (result == null);
        return result;
    }

    public boolean remove(final int key) {
        try {
            DatabaseEntry theKey =  getDBEntryKey(key);
            OperationStatus retVal = mainDb.delete(null, theKey);
            return retVal == OperationStatus.SUCCESS;
        } catch (DatabaseException dbe) {
            log.warn("Failed to remove entry " + key + " from BDB: " + dbe.toString());
            return false;
        }
    }

    public int getNumQueued() {
        return permits.availablePermits();
    }

    private int getNextFreeKey() {
        return getNextKey(nextFreeKey, false);
    }

    private int getNextGetKey() {
        return getNextKey(nextGetKey, true);
    }

    private int getNextKey(AtomicInteger key, boolean isGetKey) {
        synchronized(key) {
            int res = key.getAndIncrement();
            if (res == MAX_INDEX) {
                log.info("Keys wrapped, reset key to 0");
                key.set(0);
                if (isGetKey) {
                    nbLoopKey++;
                }
            }
            return res;
        }
    }

    private static void initEnvironment(File storage) {
        synchronized (BDBQueue.class) {
            if (dbEnvironment == null) {
                Environment tmp = null;
                try {
                    EnvironmentConfig envConfig = new EnvironmentConfig();
                    envConfig.setAllowCreate(true);
                    tmp = new Environment(storage, envConfig);
                    dbEnvironment = tmp;
                } catch (DatabaseException dbe) {
                    log.error("Failed to create the database environment " + dbe.toString());
                    throw new RuntimeException("Failed to create database environment for underlying storage " + storage);
                }
            }
        }
    }

    private Pair<Integer, Integer> lookupRealMinMax() {
        log.info("Indexes wrapped around, looking for real min/max value");
        Pair<Integer, Integer> res = null;
        Cursor cursor = null;
        int min = -1;
        int max = -1;
        try {
             cursor = mainDb.openCursor(null, null);
             DatabaseEntry key = getDBEntryKey(MAX_NB_ELEMENTS + 1);
             DatabaseEntry data = new DatabaseEntry();
             OperationStatus status = cursor.getSearchKeyRange(key, data, LockMode.READ_UNCOMMITTED);
             if (status == OperationStatus.SUCCESS) {
                 min = ((Integer) keyBinding.entryToObject(key)).intValue();
                 key = getDBEntryKey(min);
                 status = cursor.getPrev(key, data, LockMode.READ_UNCOMMITTED);
                 if (status == OperationStatus.SUCCESS) {
                     max = ((Integer) keyBinding.entryToObject(key)).intValue();
                 }
             }
        } catch (DatabaseException dbe) {
            log.error("Failure to get cursor on real min/max: " + dbe.getMessage());
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (DatabaseException ignore) {
            }
        }
        if (min == -1 || max == -1) {
            log.error("Failed to lookup for real min/maxvalues");
            return null;
        }
        res = new Pair<Integer, Integer>(new Integer(min), new Integer(max));
        return res;
    }

    private int initializeKeys() {
        int min = getMinKey();
        int max = getMaxKey();

        int nbInitialPermits = 0;
        int nextFree = 0;
        int nextGet = 0;

        switch (max) {
        //
        // No entries in the DB
        //
        case KEY_NOT_FOUND:
            nbLoopKey = 0;
            max = 0;
            nextFree = 0;
            nextGet = 0;
            break;
        //
        // There are entries in the DB but the indexes wrapped around and so
        // we need to figure out the real min/max indexes. Initialize nbLoopKey to
        // -1 so we remember to skip any missing keys-- see isKeySkipped.
        //
        case MAX_INDEX:
            nbLoopKey = -1;
            Pair<Integer, Integer> minMax = lookupRealMinMax();
            if (minMax == null) {
                throw new RuntimeException("Cannot initialize the DB, failed to get realmin/max values");
            }
            min = minMax.getFirst().intValue();
            max = minMax.getSecond().intValue();
            nextFree = max + 1;
            nextGet = min;
            nbInitialPermits =  (MAX_INDEX - min + 1) + (max + 1);
            break;
        //
        // Easy case. The min is the key to be retrieved and the max + 1 is the first key that is
        // available for insert.
        //
        default:
            nbLoopKey = 0;
            nextFree = max + 1;
            nextGet = min;
            nbInitialPermits =  max + 1 - nextGet;
            break;
        }


        log.info("Initialize keys min = " + min + ", max = " + max);
        nextFreeKey = new AtomicInteger(nextFree);
        nextGetKey = new AtomicInteger(nextGet);

        permits = new Semaphore(nbInitialPermits, true);
        log.info("Initial number of permits = " + permits.availablePermits() + ", nextFreeKey = " + nextFreeKey + ", nextGetKey = " + nextGetKey);

        return nextFree;
    }

    private boolean isKeySkipped(int key) {
        if (nbLoopKey < 0) {
            return true;
        }
        if (key < initialFreeKey && nbLoopKey == 0) {
            return true;
        }
        return false;
    }


    private Database openBDB(String dbName) {
        Database result = null;
        try {
            DatabaseConfig dbConfig = new DatabaseConfig();
            dbConfig.setAllowCreate(true);
            result = dbEnvironment.openDatabase(null, dbName, dbConfig);
            return result;
        } catch (DatabaseException dbe) {
            log.error("Failed tp open the DB");
            throw new RuntimeException(dbe);
        }
    }

    private void closeBDBs(Database...dbs) {
        try {
            for (Database db : dbs) {
                if (db != null) {
                    db.close();
                }
            }
            int nb = nbEnvironmentConsumers.decrementAndGet();
            if (nb == 0 && dbEnvironment != null) {
                dbEnvironment.close();
                dbEnvironment = null;
            }
        } catch (DatabaseException dbe) {
            log.error("Failed to close the BDB database " + dbe.toString());
        }
    }

    private DatabaseEntry getDBEntryKey(int key) {
        DatabaseEntry theKey = new DatabaseEntry();
        keyBinding.objectToEntry(key, theKey);
        return theKey;
    }


    protected boolean putWithKey(int newKey, final T it) {

        if (permits.availablePermits() > maxElements) {
            throw new RuntimeException("Reached maxium number of elements in the queue");
        }

        try {
            DatabaseEntry theKey =  getDBEntryKey(newKey);

            DatabaseEntry theData = new DatabaseEntry();
            tupleBinding.objectToEntry(it, theData);

            OperationStatus res = mainDb.put(null, theKey, theData);
            if (res == OperationStatus.SUCCESS) {
                permits.release();
                return true;
            }
        } catch (DatabaseException dbe) {
            log.warn("Failed to insert the message in the BDB for key " + newKey + ": " + dbe.toString());
        }
        log.warn("Failed to insert the message at key " + newKey);
        synchronized(skippedKeys) {
            skippedKeys.add(new Integer(newKey));
        }
        return false;
    }

    private int getMinKey() {
        return getEdge(EdgeType.EDGE_MIN);
    }

    private int getMaxKey() {
        return getEdge(EdgeType.EDGE_MAX);
    }

    private int getEdge(EdgeType type) {

        Cursor cursor = null;
        Integer retKey = new Integer(KEY_NOT_FOUND);
        try {
             cursor = mainDb.openCursor(null, null);

             DatabaseEntry key = new DatabaseEntry();
             DatabaseEntry data = new DatabaseEntry();

            OperationStatus status = OperationStatus.KEYEMPTY;

            switch (type) {
            case EDGE_MIN:
                status = cursor.getFirst(key, data, LockMode.DEFAULT);
                break;
            case EDGE_MAX:
                status = cursor.getLast(key, data, LockMode.DEFAULT);
                break;
            default:
                throw new RuntimeException("EdgeType not supported");
            }

            if (status == OperationStatus.SUCCESS) {
                retKey = (Integer) keyBinding.entryToObject(key);
            }
        } catch (DatabaseException dbe) {
            log.error("Failed to get next message from the queue:" + dbe.toString());
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (DatabaseException ignore) {
            }
        }
        return retKey.intValue();
    }

    public Pair<Integer, T> get(int key) {

        Pair<Integer, T> result = null;
        try {
            DatabaseEntry theKey =  getDBEntryKey(key);
            DatabaseEntry theData = new DatabaseEntry();
            OperationStatus retVal = mainDb.get(null, theKey, theData,
                    LockMode.DEFAULT);
            if (retVal != OperationStatus.SUCCESS) {
                return null;
            }

            Integer retKey = new Integer(key);
            T retData = (T) tupleBinding.entryToObject(theData);

            result = new Pair<Integer, T>(retKey, retData);
        } catch (DatabaseException dbe) {
            log.error("Failed to retrieve object " + key + " from BDB: " + dbe.toString());
        }
        return result;
    }

    private static class TBindingXML<T> extends TupleBinding {

        private XStream xstream;

        public TBindingXML() {
            xstream = XStreamUtils.getXStreamNoStringCache();
        }

        public void objectToEntry(Object object, TupleOutput to) {
            T myData = (T) object;
            String myDataXML = xstream.toXML(myData);
            to.writeString(myDataXML);

        }

        public Object entryToObject(TupleInput ti) {
            String myDataXML = new String(ti.readString());
            T myData = (T) xstream.fromXML(myDataXML);
            return myData;
        }
    }
}