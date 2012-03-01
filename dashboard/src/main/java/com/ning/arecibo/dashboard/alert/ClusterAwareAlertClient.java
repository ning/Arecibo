package com.ning.arecibo.dashboard.alert;

import com.google.inject.Inject;
import com.ning.arecibo.dashboard.guice.DashboardConfig;
import com.ning.arecibo.util.Logger;
import com.ning.arecibo.util.service.Selector;
import com.ning.arecibo.util.service.ServiceDescriptor;
import com.ning.arecibo.util.service.ServiceLocator;
import com.ning.arecibo.util.service.ServiceNotAvailableException;
import com.ning.arecibo.util.service.ServiceSelector;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;

public class ClusterAwareAlertClient
{
    private static final Logger log = Logger.getLogger(ClusterAwareAlertClient.class);

    private final DashboardConfig dashboardConfig;
    private final ServiceLocator serviceLocator;
    private final AlertRESTClient api;
    private final Selector selector;

    @Inject
    public ClusterAwareAlertClient(final DashboardConfig dashboardConfig,
                                   final ServiceLocator serviceLocator,
                                   final AlertRESTClient api)
    {
        this.dashboardConfig = dashboardConfig;
        this.serviceLocator = serviceLocator;
        this.api = api;

        if (StringUtils.isBlank(dashboardConfig.getAlertHostOverride())) {
            this.selector = new ServiceSelector(AlertService.NAME);
            log.info("Using prepared selector for alertService");
        }
        else {
            this.selector = null;
            log.info("Using override host/port for alert service, at: %s", dashboardConfig.getAlertHostOverride());
        }
    }

    public List<DashboardAlertStatus> getAlertStatus(final Long generationCount)
    {
        int tries = 3;
        IOException exception = null;

        while (tries > 0) {
            final String host;
            final int port;

            if (selector != null) {
                final ServiceDescriptor descriptor;
                try {
                    descriptor = serviceLocator.selectServiceAtRandom(selector);
                }
                catch (ServiceNotAvailableException e) {
                    log.warn("No alert servers available");
                    return null;
                }

                host = descriptor.getProperties().get(AlertService.HOST_KEY);
                port = Integer.parseInt(descriptor.getProperties().get(AlertService.PORT_KEY));
            }
            else {
                final String[] hostAndPort = dashboardConfig.getAlertHostOverride().split(":");
                host = hostAndPort[0];
                port = hostAndPort.length > 0 ? Integer.parseInt(hostAndPort[1]) : 80;
            }

            try {
                return api.getAlertStatus(host, port, generationCount);
            }
            catch (IOException e) {
                log.warn(e, "Error talking to alert server %s:%d to get alert status", host, port);

                --tries;
                exception = e;
            }
        }

        log.warn(exception, "Cannot get alert status");
        return null;
    }
}
