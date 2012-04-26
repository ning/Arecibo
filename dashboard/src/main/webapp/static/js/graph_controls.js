/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

function shiftLeft(graphId) {
    var graph = getGraphMetaObjectById(graphId);
    var hosts = Set.elements(graph.hosts);
    var nbSamples = screen.width;

    var url = shiftLeftUrl(hosts, graph.sampleCategory, graph.sampleKind, graph.startDate, graph.endDate, nbSamples);
    callArecibo(url, 'refreshGraph');
}

function shiftRight(graphId) {
    var graph = getGraphMetaObjectById(graphId);
    var hosts = Set.elements(graph.hosts);
    var nbSamples = screen.width;

    var url = shiftRightUrl(hosts, graph.sampleCategory, graph.sampleKind, graph.startDate, graph.endDate, nbSamples);
    callArecibo(url, 'refreshGraph');
}

function zoomIn(graphId) {
    var graph = getGraphMetaObjectById(graphId);
    var hosts = Set.elements(graph.hosts);
    var nbSamples = screen.width;

    var url = zoomInUrl(hosts, graph.sampleCategory, graph.sampleKind, graph.startDate, graph.endDate, nbSamples);
    callArecibo(url, 'refreshGraph');
}

function zoomOut(graphId) {
    var graph = getGraphMetaObjectById(graphId);
    var hosts = Set.elements(graph.hosts);
    var nbSamples = screen.width;

    var url = zoomOutUrl(hosts, graph.sampleCategory, graph.sampleKind, graph.startDate, graph.endDate, nbSamples);
    callArecibo(url, 'refreshGraph');
}

// Function invoked when the user clicks on the button
function realtime(graphId) {
    var graph = getGraphMetaObjectById(graphId);

    // Is there already a periodic update? If so just cancel it
    if (graph.periodicXhrTimeout) {
        window.clearTimeout(graph.periodicXhrTimeout);
        graph.periodicXhrTimeout = null;
        return;
    }

    doRealtimeUpdate(graphId);
}

// Function invoked when the user clicks on the button and during the periodic updates
function doRealtimeUpdate(graphId) {
    var graph = getGraphMetaObjectById(graphId);
    var hosts = Set.elements(graph.hosts);
    var nbSamples = screen.width;

    var url = realtimeUrl(hosts, graph.sampleCategory, graph.sampleKind, graph.startDate, graph.endDate, nbSamples);
    callPeriodicArecibo(graphId, url, 'refreshGraph');
}

// Build the collector Url that shifts left the timeseries
function shiftLeftUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 50% left
    var deltaMillis = getDeltaForGraph(fromDate, toDate);

    removeMillis(fromDate, deltaMillis);
    removeMillis(toDate, deltaMillis);

    return buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that shifts right the timeseries
function shiftRightUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 50% right
    var deltaMillis = getDeltaForGraph(fromDate, toDate);

    addMillis(fromDate, deltaMillis);
    addMillis(toDate, deltaMillis);

    return buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that zooms in the timeseries
function zoomInUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 25% on both sides
    var deltaMillis = getDeltaForGraph(fromDate, toDate) / 2;

    addMillis(fromDate, deltaMillis);
    removeMillis(toDate, deltaMillis);

    return buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that zooms out the timeseries
function zoomOutUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 25% on both sides
    var deltaMillis = getDeltaForGraph(fromDate, toDate) / 2;

    removeMillis(fromDate, deltaMillis);
    addMillis(toDate, deltaMillis);

    return buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that shifts right the timeseries for the realtime update
function realtimeUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // 10 seconds
    var deltaMillis = 10000;

    addMillis(fromDate, deltaMillis);
    addMillis(toDate, deltaMillis);

    return buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Get the middle of a date interval
function getDeltaForGraph(startDate, endDate) {
    return (endDate.getTime() - startDate.getTime())/2;
}

// Build the collector Url for a given host and sample kind
function buildHostSampleUrl(hosts, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    var url = '/rest/1.0/host_samples?' +
            'category_and_sample_kind=' + sampleCategory + ',' + sampleKind + '&' +
            'from=' + ISODateString(fromDate) + '&' +
            'to=' + ISODateString(toDate) + '&' +
            'output_count=' + nbSamples;

    for (var i in hosts) {
        url += '&host=' + hosts[i];
    }

    return url;
}