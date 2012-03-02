Overview
--------

1. To build

    mvn clean install


Collector
---------

The collector gathers and stores data sent by the agents to the datastore (MySQL by default). It supports multiple APIs, including REST over HTTP and UDP.

Before starting the collector, you need to create a database first, e.g.:

    ~ > echo 'create database arecibo' | mysql -u root -p
    Enter password:
    ~ > cat collector/src/main/resources/collector.sql | mysql -u root -p arecibo
    Enter password:


To start the collector, assuming MySQL is running locally:

    java \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.port=8990 \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Darecibo.events.collector.db.url=jdbc:mysql://127.0.0.1:3306/arecibo \
        -Darecibo.events.collector.db.user=root \
        -Darecibo.events.collector.db.password=root
        -jar target/arecibo-collector-*-jar-with-dependencies.jar


To send data via the REST API, send JSON payloads to /xn/rest/1.0/event, e.g. (send 2 data points):

    echo "
    {
        \"timestamp\": $(date +%s)000,
        \"eventType\": \"testEvent\",
        \"sourceUUID\": \"550e8400-e29b-41d4-a716-446655440000\",
        \"min_heapUsed\": $RANDOM.515698888E9,
        \"max_heapUsed\": $RANDOM.835511784E9
    }" | \
    curl -v -H'Content-Type: application/json' -XPOST -d@- http://127.0.0.1:8088/xn/rest/1.0/event


Data is exposed in JSON format, e.g.:

    curl -v http://127.0.0.1:8088/rest/1.0/hosts

See `com.ning.arecibo.collector.resources` for available endpoints.

For convenience, there is a Java library to access the data (see the collector-client-support module).

Dashboard
---------

The dashboard module exposes graphs of collected metrics. It relies on a collector to access the data (using the collector REST api) and can be configured to use the alertmanager (disabled by default).

To start the dashboard, assuming the collector is at `http://127.0.0.1:8088`:

    mvn \
        -Dcom.sun.management.jmxremote.authenticate=false \
        -Dcom.sun.management.jmxremote.port=8989 \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Darecibo.collectorClient.collectorUri=http://127.0.0.1:8088 \
        jetty:run

You can test it by hitting the dashboard's endpoints, e.g.:

    curl -v http://127.0.0.1:8080/rest/1.0/hosts

Note that the output is not JSON, but JSONP, for the dashboard AJAX requests. To get straight JSON, use the collector endpoints instead, e.g.:

    curl -v http://127.0.0.1:8088/rest/1.0/hosts
