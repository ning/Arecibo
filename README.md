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
        -Darecibo.events.collector.db.url=jdbc:mysql://127.0.0.1:3306/arecibo \
        -Darecibo.events.collector.db.user=root \
        -Darecibo.events.collector.db.password=root
        -jar target/arecibo-collector-*-jar-with-dependencies.jar

Data is exposed in JSON format, e.g.:

    curl -v http://127.0.0.1:8088/rest/1.0/hosts

See `com.ning.arecibo.collector.resources` for available endpoints.

For convenience, there is a Java library to access the data (see the collector-client-support module).

Dashboard
---------

The dashboard module exposes graphs of collected metrics. It relies on a collector to access the data (using the collector REST api) and can be configured to use the alertmanager (disabled by default).

To start the dashboard, assuming the collector is at `http://127.0.0.1:8088`:

    mvn -Darecibo.collectorClient.collectorUri=http://127.0.0.1:8088 jetty:run

You can test it by hitting the dashboard's endpoints, e.g.:

    curl -v http://127.0.0.1:8080/rest/1.0/hosts

Note that the output is not JSON, but JSONP, for the dashboard AJAX requests. To get straight JSON, use the collector endpoints instead, e.g.:

    curl -v http://127.0.0.1:8088/rest/1.0/hosts
