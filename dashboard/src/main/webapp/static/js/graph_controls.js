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

// Build the collector Url that shifts left the timeseries
function shiftLeft(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 50% left
    var deltaMillis = getDeltaForGraph(fromDate, toDate);

    removeMillis(fromDate, deltaMillis);
    removeMillis(toDate, deltaMillis);

    return buildHostSampleUrl(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that shifts right the timeseries
function shiftRight(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 50% right
    var deltaMillis = getDeltaForGraph(fromDate, toDate);

    addMillis(fromDate, deltaMillis);
    addMillis(toDate, deltaMillis);

    return buildHostSampleUrl(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that zooms in the timeseries
function zoomIn(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 25% on both sides
    var deltaMillis = getDeltaForGraph(fromDate, toDate) / 2;

    addMillis(fromDate, deltaMillis);
    removeMillis(toDate, deltaMillis);

    return buildHostSampleUrl(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Build the collector Url that zooms out the timeseries
function zoomOut(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    // Shift 25% on both sides
    var deltaMillis = getDeltaForGraph(fromDate, toDate) / 2;

    removeMillis(fromDate, deltaMillis);
    addMillis(toDate, deltaMillis);

    return buildHostSampleUrl(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples);
}

// Get the middle of a date interval
function getDeltaForGraph(startDate, endDate) {
    return (endDate.getTime() - startDate.getTime())/2;
}

// Build the collector Url for a given host and sample kind
function buildHostSampleUrl(host, sampleCategory, sampleKind, fromDate, toDate, nbSamples) {
    return '/rest/1.0/host_samples?' +
            'host=' + host + '&' +
            'category_and_sample_kind=' + sampleCategory + ',' + sampleKind + '&' +
            'from=' + ISODateString(fromDate) + '&' +
            'to=' + ISODateString(toDate) + '&' +
            'output_count=' + nbSamples;
}