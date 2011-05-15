package com.ning.arecibo.util.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.codec.binary.Hex;
import com.google.inject.Inject;
import com.ning.arecibo.util.NamedThreadFactory;

/**
 * Chooses a service based on a consistent hashing algorithm. For a given key, the same server will be chosen
 * while it is available.
 */
public class ConsistentHashingServiceChooser implements ServiceListener, ServiceChooser
{
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicInteger serviceCount = new AtomicInteger(0);
        
    private final ServiceLocator serviceLocator;
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("NodeChooser listener"));
    private final TreeMap<String, ServiceDescriptor> descriptorsByHash = new TreeMap<String, ServiceDescriptor>();

    private final ConsistentHashingConfig config;
    private final Selector selector;

    /**
     * @param serviceLocator the cluster to select from
     * @param virtualNodes number of virtual nodes in addition to the node itself
     * @param selector the selector to decide what services the ConsistentHashingServiceChooser is selecting over
     */
    @Inject
    public ConsistentHashingServiceChooser(ConsistentHashingConfig config,
                                           ServiceLocator serviceLocator,
                                           @ConsistentHashingSelector Selector selector)
    {
        this.config = config;
        this.serviceLocator = serviceLocator;
        this.selector = selector;       
    }

    private String computeNodeHash(String key)
    {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 provider not found");
        }

        md5.update(key.getBytes());
        byte[] bytes = md5.digest();

        // we need a perfect hash (i.e. one-to-one mapping between keys and hashes) so that we can identify
        // the node that handles a given key unambiguously. We do this by appending the key to the hashcode.
        StringBuilder result = new StringBuilder(32 + key.length() + 1); // 32 for md5, 1 for colon, rest for key
        result.append(Hex.encodeHex(bytes));
        result.append(':');
        result.append(key);
        return result.toString();
    }

    private List<String> getVirtualNodeHashes(ServiceDescriptor sd)
    {
        List<String> keys = new ArrayList<String>(config.getVirtualNodes());

        // this helps ensure that findClosest(getUuid().toString()) returns the node with the same key
        // Usually, relying on arbitrary hashes is enough, but it may be useful to be able to get a node more predictably
        // and fall back to other nodes if that one is not available (e.g., where the keys represent "partitions")
        keys.add(computeNodeHash(sd.getUuid().toString()));

        for (int i = 0; i < config.getVirtualNodes(); ++i) {
            keys.add(computeNodeHash(sd.getUuid().toString() + "-" + i));
        }

        return keys;
    }

    public void start()
    {
        boolean wasStarted = started.getAndSet(true);

        if (!wasStarted) {
            serviceLocator.registerListener(selector, executor, this);
        }
    }

    public void onRemove(ServiceDescriptor sd)
    {
        List<String> keys = getVirtualNodeHashes(sd);

        synchronized (descriptorsByHash) {
            for (String key : keys) {
                descriptorsByHash.remove(key);
            }
        }

        serviceCount.decrementAndGet();
    }

    public void onAdd(ServiceDescriptor sd)
    {
        List<String> keys = getVirtualNodeHashes(sd);

        synchronized (descriptorsByHash) {
            for (String key : keys) {
                descriptorsByHash.put(key, sd);
            }
        }

        serviceCount.incrementAndGet();
    }

    public void stop()
    {
        boolean wasStopped = started.getAndSet(false);

        if (!wasStopped) {
            serviceLocator.unregisterListener(this);
            executor.shutdown();
        }
    }
    
    public ServiceDescriptor findClosest(String key)
    {
        if (!started.get()) {
            throw new IllegalStateException("Not yet started");
        }

        String hash = computeNodeHash(key);

        synchronized (descriptorsByHash) {
            Map.Entry<String, ServiceDescriptor> entry = descriptorsByHash.ceilingEntry(hash);

            if (entry == null) {
                entry = descriptorsByHash.firstEntry();
            }

            if (entry != null) {
                return entry.getValue();
            }
        }

        return null;
    }
    
    public Collection<ServiceDescriptor> findNClosest(String key, int targetCount)
    {
        if (!started.get()) {
            throw new IllegalStateException("Not yet started");
        }

        String hash = computeNodeHash(key);

        synchronized (descriptorsByHash) {
            SortedMap<String, ServiceDescriptor> candidates = descriptorsByHash.tailMap(hash);
            LinkedHashSet<ServiceDescriptor>     result     = new LinkedHashSet<ServiceDescriptor>();

            for (ServiceDescriptor serviceDesc : candidates.values()) {
                if (result.add(serviceDesc)) {
                    if (result.size() == targetCount) {
                        break;
                    }
                }
            }
            if (result.size() < targetCount) {
                candidates = descriptorsByHash.headMap(hash, false);
                for (ServiceDescriptor serviceDesc : candidates.values()) {
                    if (result.add(serviceDesc)) {
                        if (result.size() == targetCount) {
                            break;
                        }
                    }
                }
            }
            return result;
        }
    }

    public ServiceDescriptor getResponsibleService(String index)
    {
        return findClosest(index);
    }
}
