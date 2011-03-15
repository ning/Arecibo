package com.ning.arecibo.agent.datasource.http;

import java.util.Collection;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import com.ning.arecibo.agent.config.Config;
import com.ning.arecibo.agent.config.ConfigException;
import com.ning.arecibo.agent.config.http.HTTPResponseCheckConfig;
import com.ning.arecibo.agent.datasource.DataSource;
import com.ning.arecibo.agent.datasource.DataSourceException;
import com.ning.arecibo.agent.datasource.DataSourceType;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;

public class HTTPResponseCheckDataSource implements DataSource {

    public static final String RESPONSE_STATUS_CODE = "responseStatusCode";
    public static final String RESPONSE_STATUS_TIME_MS = "responseStatusTimeMs";
    public static final String RESPONSE_STATUS_MESSAGE_MATCH = "responseStatusMessageMatch";
    public static final String RESPONSE_BODY_TIME_MS = "responseBodyTimeMs";
    public static final String RESPONSE_BODY_SIZE = "responseBodySize";
    public static final String RESPONSE_BODY_MATCH = "responseBodyMatch";
    public static final String RESPONSE_MESSAGE = "responseMessage";
    public static final String TEMP_RESPONSE_LOCATION_HEADER = "tempResponseLocationHeader";

    public static final int FAILED_RESPONSE_STATUS_CODE = -1;

    // should make this a param
    public static final int MAX_REDIRECTS = 5;

    // 10 ^ 6
    public static final double NANOS_PER_MILLI = 1000000.0;

    //TODO: Inject this as a param?
    private static final int RESPONSE_BUFFER_SIZE = 1024;

    private final String host;
    private final int port;
    private final int timeout;
    private final String httpUserAgentString;
    private final String httpProxyHost;
    private final int httpProxyPort;

    private final String uri;
    private final boolean ignoreBodyFlag;
    private final boolean followRedirectsFlag;
    private final String virtualHost;
    private final Map<String, Collection<String>> extraHeaders;
    private final String matchStatusMessageSubString;
    private final String matchBodySubString;

    private final Map<String, String> configHashKeyMap;

    private volatile AsyncHttpClient httpClient = null;

    public HTTPResponseCheckDataSource(Config config, int timeout, String httpUserAgentString, String httpProxyHost, int httpProxyPort)
        throws DataSourceException {

        if(!(config instanceof HTTPResponseCheckConfig)) {
            throw new DataSourceException("HTTPResponseCheckDataSource must be initialized with an instance of HTTPResponseCheckConfig");
        }
        HTTPResponseCheckConfig httpResponseCheckConfig = (HTTPResponseCheckConfig)config;

        this.host = config.getHost();
        this.port = httpResponseCheckConfig.getPort();

        // convert to ms from secs
        this.timeout = 1000 * timeout;

        this.httpUserAgentString = httpUserAgentString;
        this.httpProxyHost = httpProxyHost;
        this.httpProxyPort = httpProxyPort;

        this.uri = httpResponseCheckConfig.getUri();
        this.ignoreBodyFlag = httpResponseCheckConfig.getIgnoreBodyFlag();
        this.followRedirectsFlag = httpResponseCheckConfig.getFollowRedirectsFlag();
        this.virtualHost = httpResponseCheckConfig.getVirtualHost();
        this.extraHeaders = httpResponseCheckConfig.getExtraHeaders();
        this.matchStatusMessageSubString = httpResponseCheckConfig.getMatchStatusMessageSubString();
        this.matchBodySubString = httpResponseCheckConfig.getMatchBodySubString();
        
        this.configHashKeyMap = new HashMap<String,String>();
    }

    @Override
    public void initialize() throws DataSourceException {
        Builder builder = new AsyncHttpClientConfig.Builder();

        builder.setConnectionTimeoutInMs(timeout)
            .setRequestTimeoutInMs(timeout)
            .setUserAgent(httpUserAgentString);
        // followRedirects doesn't play nice with proxy's, logic handled below instead
        //builder.setFollowRedirects(followRedirectsFlag);
        if (httpProxyHost != null && httpProxyHost.length() > 0) {
            builder.setProxyServer(new ProxyServer(httpProxyHost, httpProxyPort));
        }

        this.httpClient = new AsyncHttpClient(builder.build());

    }

    @Override
    public boolean isInitialized() {
        return this.httpClient != null;
    }

    @Override
    public void closeResources() throws DataSourceException {
        this.httpClient.close();
        this.httpClient = null;
    }

    @Override
    public boolean canExpandConfigs() {
        return true;
    }

    @Override
    public Map<String, Config> expandConfigs(Map<String, Config> configs) throws DataSourceException {

        // for each config, create 1 for each attribute type we want to include
        List<Config> toAdd = new ArrayList<Config>();
        for(Config config:configs.values()) {
            try {
                toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_STATUS_CODE));
                toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_STATUS_TIME_MS));
                toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_MESSAGE));

                if(!ignoreBodyFlag) {
                    toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_BODY_SIZE));
                    toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_BODY_TIME_MS));
                }

                if(matchStatusMessageSubString != null) {
                    toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_STATUS_MESSAGE_MATCH));
                }

                if(matchBodySubString != null) {
                    toAdd.add(new HTTPResponseCheckConfig((HTTPResponseCheckConfig)config,RESPONSE_BODY_MATCH));
                }
            }
            catch(ConfigException cEx) {
                throw new DataSourceException("Problem expanding configs",cEx);
            }
        }

        configs.clear();
        for(Config config:toAdd) {
            configs.put(config.getConfigHashKey(),config);
        }

        return configs;
    }

    @Override
    public void prepareConfig(Config config) throws DataSourceException {
        this.configHashKeyMap.put(config.getEventAttributeType(),config.getConfigHashKey());
    }

    @Override
    public void finalizePreparation() throws DataSourceException {
    }

    @Override
    public Map<String, Object> getValues()  throws DataSourceException {

        final HashMap<String,Object> values = new HashMap<String,Object>();

        try {
            String url = String.format("http://%s:%d%s", host, port, uri);

            BoundRequestBuilder builder = httpClient.prepareGet(url);

            if (extraHeaders != null) {
                builder.setHeaders(extraHeaders);
            }
            if (virtualHost != null) {
                builder.setVirtualHost(virtualHost);
            }

            Integer statusCode = performRequest(builder, values);

            int redirectCount = 0;
            while (statusCode >= 300 && statusCode <= 399 && this.followRedirectsFlag && redirectCount++ < MAX_REDIRECTS) {
                // don't use virtual host on redirect
                // this is actually a bug in the commons_httpclient 3.x lib, it keeps reusing the same virtual host
                // if you use it's built in followRedirect feature
                builder.setVirtualHost(null);

                String locationHeader = (String)values.get(TEMP_RESPONSE_LOCATION_HEADER);
                URL redirectUrl = new URL(locationHeader);
                String locationHost = redirectUrl.getHost();
                int locationPort = redirectUrl.getPort();

                if (locationPort != 80) {
                    url = String.format("http://%s:%d%s", locationHost, locationPort, uri);
                }
                else {
                    url = String.format("http://%s%s", locationHost, uri);
                }

                builder.setUrl(url);

                statusCode = performRequest(builder, values);
            }

        }
        catch (IOException ioEx) {

            Throwable t = ioEx;
            while(t.getCause() != null) {
                t = t.getCause();
            }

            values.put(configHashKeyMap.get(RESPONSE_MESSAGE),t.toString());
            values.put(configHashKeyMap.get(RESPONSE_STATUS_CODE),FAILED_RESPONSE_STATUS_CODE);
        }

        return values;
    }

    private Integer performRequest(final BoundRequestBuilder builder, final Map<String,Object> values) throws IOException {

        final long startNanos = System.nanoTime();

        Response response;

        try {
            response = builder.execute().get();
        }
        catch (IOException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new IOException("Could not perform http request", ex);
        }

        // need to take end timing samples inside the response handler, avoid the extra time it takes
        // with cleaning up the response, via releaseClient, in the case where we don't read the response body

        int statusCode = response.getStatusCode();
        values.put(configHashKeyMap.get(RESPONSE_STATUS_CODE),statusCode);

        String locationHeader = response.getHeader("Location");
        if(locationHeader != null) {
            values.put(TEMP_RESPONSE_LOCATION_HEADER,locationHeader);
        }

        String statusText = URLDecoder.decode(response.getStatusText(),"UTF-8");
        values.put(configHashKeyMap.get(RESPONSE_MESSAGE),statusText);

        if(matchStatusMessageSubString != null) {
            int matchResult = statusText.contains(matchStatusMessageSubString) ? 1 : 0;

            values.put(configHashKeyMap.get(RESPONSE_STATUS_MESSAGE_MATCH),matchResult);
        }

        // get the timing for the status response
        long endNanos = System.nanoTime();
        double responseMillis = (double)(endNanos - startNanos) / NANOS_PER_MILLI;
        values.put(configHashKeyMap.get(RESPONSE_STATUS_TIME_MS),responseMillis);

        // read the body, if not to be ignored
        if(!ignoreBodyFlag) {
            BufferedReader bufferedReader = null;
            try {
                InputStream bodyStream = response.getResponseBodyAsStream();
                bufferedReader = new BufferedReader(new InputStreamReader(bodyStream),RESPONSE_BUFFER_SIZE);

                StringBuilder matchingSb = null;
                boolean matchFound = false;

                char[] buf = new char[RESPONSE_BUFFER_SIZE];
                int numRead;
                long numTotalRead = 0L;
                while((numRead = bufferedReader.read(buf,0,RESPONSE_BUFFER_SIZE)) > -1) {
                    numTotalRead += numRead;

                    // check for a body sub string match
                    if(matchBodySubString != null && !matchFound) {
                        if(matchingSb == null) {
                            matchingSb = new StringBuilder();
                        }

                        matchingSb.append(buf);
                        if(matchingSb.toString().contains(matchBodySubString)) {
                            matchFound = true;
                        }
                        else {
                            // delete all but enough chars at the end of the curr segment that could be
                            // patched with the next segment
                            if(matchingSb.length() > matchBodySubString.length()) {
                                matchingSb.delete(0,matchingSb.length() - matchBodySubString.length());
                            }
                        }
                    }
                }

                values.put(configHashKeyMap.get(RESPONSE_BODY_SIZE),numTotalRead);

                if(matchBodySubString != null) {
                    values.put(configHashKeyMap.get(RESPONSE_BODY_MATCH),matchFound?1:0);
                }
            }
            finally {
                if(bufferedReader != null) {
                    bufferedReader.close();
                }
            }

            // get the timing for the body response
            endNanos = System.nanoTime();
            responseMillis = (double)(endNanos - startNanos) / NANOS_PER_MILLI;
            values.put(configHashKeyMap.get(RESPONSE_BODY_TIME_MS),responseMillis);
        }

        return response.getStatusCode();
    }

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.HTTPResponseCheck;
    }

    public static boolean matchesConfig(Config config) {
        return config instanceof HTTPResponseCheckConfig;
    }

}
