package com.ning.arecibo.event.publisher;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.Managed;
import com.google.inject.Inject;
import com.ning.arecibo.event.transport.EventService;
import com.ning.arecibo.event.transport.EventServiceRESTClient;
import com.ning.arecibo.event.transport.EventServiceUDPClient;
import com.ning.arecibo.event.transport.JavaEventSerializer;
import com.ning.arecibo.util.NamedThreadFactory;
import com.ning.arecibo.util.UUIDUtil;
import com.ning.arecibo.util.cron.CronJob;
import com.ning.arecibo.util.cron.JMXCronTaskMaster;
import com.ning.arecibo.util.service.ConsistentHashingSelector;
import com.ning.arecibo.util.service.ConsistentHashingServiceChooser;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.http.client.AsyncHttpClient;

public class AreciboEventServiceChooser implements EventServiceChooser
{
    private final ServiceLocator serviceLocator;
    private final ConcurrentHashMap<UUID, EventService> cache = new ConcurrentHashMap<UUID, EventService>();
    private final ConsistentHashingServiceChooser magic;
    private final EventServiceRESTClient restClient;
    private final Selector selector;
    private final EventServiceUDPClient udpClient;
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("EventServiceChooser"));

    @Inject
    public AreciboEventServiceChooser(ServiceLocator serviceLocator,
                                      @ConsistentHashingSelector Selector selector,
                                      ConsistentHashingServiceChooser magic,
                                      MBeanExporter exporter,
                                      @EventSenderType String senderType,
                                      AsyncHttpClient httpClient) throws IOException
    {
        this.serviceLocator = serviceLocator;
        this.selector = selector;
        this.serviceLocator.startReadOnly();
        this.restClient = new EventServiceRESTClient(httpClient, new JavaEventSerializer(), senderType);
        this.udpClient = new EventServiceUDPClient(new JavaEventSerializer(), senderType);
        this.magic = magic;

        JMXCronTaskMaster master = new JMXCronTaskMaster(Executors.newScheduledThreadPool(1), exporter);
        long jobInterval = Long.getLong("xn.event.chooser.invalidateCacheIntervalInMinutes", 60L);

        CronJob job = master.exportNewFixedDelayCronJob(Math.abs(jobInterval), TimeUnit.MINUTES, "ning.realtime:type=EventServiceChooser,name=CacheRecycler", new Runnable()
        {
            public void run()
            {
                cache.clear();
            }
        });

        if (jobInterval > 0) {
            job.start() ;
        }
    }

    public Set<ServiceDescriptor> getAllServiceDescriptors()
    {
        return serviceLocator.selectServices(selector);
    }

    public void start()
    {
        magic.start();
        serviceLocator.registerListener(selector, executor, this);
    }

    public void stop()
    {
        magic.stop();
        serviceLocator.unregisterListener(this);

        executor.shutdown();
    }

    @Managed
    public void clearEventServiceCache()
    {
        cache.clear();
    }

    public EventService choose(UUID uuid) throws IOException
    {
        EventService es = cache.get(uuid);
        if (es == null) {
            ServiceDescriptor sd = magic.findClosest(uuid.toString());
            if (sd != null) {
                es = new AreciboEventService(this, sd, restClient, udpClient);
                cache.put(uuid, es);
                return es;
            }
            else {
                throw new IOException("No RealtimeEventService available !");
            }
        }
        else {
            return es;
        }
    }

    public void invalidate(UUID uuid)
    {
        cache.remove(uuid);
    }

    @Managed
    public String getCachedHost(String key)
    {
        UUID uuid = UUIDUtil.md5UUID(key) ;
        if ( cache.containsKey(uuid) )
        {
            return ((AreciboEventService)cache.get(uuid)).getServiceDescriptor().getProperty(EventService.HOST) ;
        }
        return null;
    }

    public ServiceDescriptor getServiceDescriptor(String key)
    {
        return magic.findClosest(UUIDUtil.md5UUID(key).toString());
    }

    @Managed
    public String getHost(String key)
    {
        return magic.findClosest(UUIDUtil.md5UUID(key).toString()).getProperty(EventService.HOST) ;
    }

    public void invalidate(EventService service)
    {
        for (Iterator<Map.Entry<UUID, EventService>> i = cache.entrySet().iterator(); i.hasNext();) {
            Map.Entry<UUID, EventService> entry = i.next();
            if (entry.getValue().equals(service)) {
                i.remove();
            }
        }
    }

    public void onRemove(ServiceDescriptor sd)
    {
        cache.clear();
    }

    public void onAdd(ServiceDescriptor sd)
    {
        cache.clear();
    }

    public UUID getServiceUUID(UUID uuid)
    {
        ServiceDescriptor sd = magic.getResponsibleService(uuid.toString());
        return sd == null ? null : sd.getUuid() ;
    }
}