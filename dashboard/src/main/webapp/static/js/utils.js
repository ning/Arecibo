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

function callArecibo(uri, callback, opts) {
    var ajax_opts = {
        url: window.arecibo['uri'] + uri,
        dataType: "jsonp",
        cache : false,
        jsonp : "callback",
        jsonpCallback: callback,
        // Hacky error handling for JSONP requests
        timeout : 50000
    }

    if (!(opts === undefined)) {
        for (var attrname in opts) {
            ajax_opts[attrname] = opts[attrname];
        }
    }

    // Populate the data
    console.log("Calling " + ajax_opts.url);
    $.ajax(ajax_opts);
}

function initializeUI() {
    // Webkit browsers only
    if (!window.location.origin) {
        window.location.origin = window.location.protocol + "//" + window.location.host;
    }

    // See http://bugs.jquery.com/ticket/8338 - this is required for the Ajax feedback functions
    jQuery.ajaxPrefilter(function(options) {
        options.global = true;
    });

    // Setup the loading indicator for Ajax calls
    $('#spinnerDiv')
        .hide()  // hide it initially
        .ajaxStart(function() {
            $(this).show();
        })
        .ajaxStop(function() {
            $(this).hide();
        });

    // Setup the error messages alert for Ajax calls
    $('#errorDiv')
        .hide()  // hide it initially
        .ajaxError(function(event, jqXHR, settings) {
            var message;
            if (jqXHR.status === 0) {
                message = 'Unable to connect to the remote host.';
            } else if (jqXHR.status == 404) {
                message = 'Requested resource not found [404].';
            } else if (jqXHR.status == 500) {
                message = 'Internal Server Error [500].';
            } else if (jqXHR.exception === 'parsererror') {
                message = 'Unable to parse the JSON response.';
            } else if (jqXHR.exception === 'timeout') {
                message = 'Connection timeout.';
            } else if (jqXHR.exception === 'abort') {
                message = 'Ajax request aborted.';
            } else {
                message = 'Uncaught Error. ' + jqXHR.responseText;
            }

            $(this).show();
            $(this).append("<p>Error requesting " + settings.url + ". " + message + "<p>");
            event.preventDefault();
        });
}

/*
 * Convert a JS Date object to an ISO String
 *
 * @param {Date}  d   The Date object to convert
 */
function ISODateString(d) {
     function pad(n){
         return n < 10 ? '0' + n : n
     }

     return d.getUTCFullYear() + '-'
     + pad(d.getUTCMonth() + 1) + '-'
     + pad(d.getUTCDate()) + 'T'
     + pad(d.getUTCHours()) + ':'
     + pad(d.getUTCMinutes()) + ':'
     + pad(d.getUTCSeconds()) + 'Z'
}

/*
 * Find a query parameter value in a query string
 *
 * @param {String}  name    The query parameter name to look for
 * @param {String}  query   The query parameters string
 */
function getParameterByName(name, query) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(query);

    if (results == null) {
        return null;
    } else {
        return decodeURIComponent(results[1].replace(/\+/g, " "));
    }
}

/*
 * Add milliseconds to a date
 *
 * @param {Date}    date            The original date
 * @param {Integer} milliseconds    The number of milliseconds to add
 */
function addMillis(date, milliseconds) {
    date.setTime(date.getTime() + milliseconds);
}

/*
 * Substract milliseconds to a date
 *
 * @param {Date}    date            The original date
 * @param {Integer} milliseconds    The number of milliseconds to substract
 */
function removeMillis(date, milliseconds) {
    date.setTime(date.getTime() - milliseconds);
}