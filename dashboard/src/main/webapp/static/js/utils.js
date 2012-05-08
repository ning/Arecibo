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

function callPeriodicArecibo(graphId, uri, callback, opts) {
    callArecibo(uri, callback, {
        complete: function(XMLHttpRequest) {
            var graph = getGraphMetaObjectById(graphId);
            graph.periodicXhrTimeout = window.setTimeout(function() {
                doRealtimeUpdate(graphId);
            }, 10000);
        }
    })
}

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

    // Serialize the callbacks to avoid weird UI errors
    if (window.arecibo.xhr) {
        window.arecibo.xhr.abort();
    }

    // Populate the data
    //console.log("Calling " + ajax_opts.url);
    $.ajax(ajax_opts);
}

function initializeUI() {
    // Webkit browsers only
    if (!window.location.origin) {
        window.location.origin = window.location.protocol + "//" + window.location.host;
    }

    // Dashboard Configuration
    window.arecibo = {
        // Current Ajax request - only one at a time
        xhr: null,
        // Current timeout on the error div
        errorDivTimeout: null,
        // Current timeout for the realtime updater
        periodicXhrTimeout: null,
        uri: window.location.origin // e.g. 'http://127.0.0.1:8080'
    }

    // See http://bugs.jquery.com/ticket/8338 - this is required for the Ajax feedback functions
    jQuery.ajaxPrefilter(function(options) {
        options.global = true;
    });

    $(document).ajaxStop(function() {
        window.arecibo.xhr = null;
    });

    // Setup the loading indicator for Ajax calls
    $('#spinnerDiv')
        .hide()  // hide it initially
        .ajaxStart(function() {
            $(this).modal('show');
        })
        .ajaxStop(function() {
            $(this).modal('hide');
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

            // If there was a scheduled timeout, cancel it
            if (window.arecibo.errorDivTimeout) {
                window.clearTimeout(window.arecibo.errorDivTimeout);
            }

            $(this).show();
            $(this).html("<p>Error requesting " + settings.url + ". " + message + "<p>");

            // Hide the error message after a while
            window.arecibo.errorDivTimeout = window.setTimeout(function() {
                $('#errorDiv').hide();
            }, 5000);

            event.preventDefault();
        });
}

/*
 * Convert a JS Date object to an ISO String
 *
 * @param {Date}  d   The Date object to convert
 */
function ISODateString(d) {
    if (!d || !d.getUTCFullYear) {
        return '';
    }

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

function sort(array, key) {
    return array.sort(function(a, b) {
        var x = a[key];
        var y = b[key];
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
}

function sort2(array, key1, key2) {
    return array.sort(function(a, b) {
        var x = a[key1][key2];
        var y = b[key1][key2];
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });
}

/*
 * Simple Set implementation
 */
var Set = new function() {
    this.makeSet = function(objs, key) {
        var set = {};
        if (objs == undefined) {
            return set;
        }

        for (var i = 0; i < objs.length; i++) {
            this.add(set, objs[i][key]);
        }

        return set;
    }

    this.add = function(s, item) {
        if (item) {
            s[item] = true;
        }
    }

    this.remove = function(s, item) {
        if (item) {
            delete(s[item]);
        }
    }

    this.contains = function(s, item) {
        return s != undefined && s[item] === true;
    }

    this.equals = function(s1, s2) {
        return (this.size(this.symdiff(s1, s2)) == 0);
    }

    this.elements = function(s) {
        var elts = [];
        if (s == undefined) {
            return elts;
        }

        for (var item in s) {
            if (this.contains(s, item)) {
                elts.push(item);
            }
        }

        return elts;
    }

    this.dup = function(s, copy) {
        for (var item in s) {
            if (this.contains(s, item)) {
                copy[item] = true;
            }
        }
    }

    this.diff = function(s1, s2) {
        var diff = {};
        this.dup(s1, diff);
        for (var item in s2) {
            if (this.contains(s2, item)) {
                delete diff[item];
            }
        }

        return diff;
    }

    this.inter = function(s1, s2) {
        var i = {};
        for (var item in s1) {
            if (s1[item] === true && s2[item] === true) {
                i[item] = true;
            }
        }

        return i;
    }

    this.symdiff = function(s1, s2) {
        var diff1 = this.diff(s1, s2);
        var diff2 = this.diff(s2, s1);
        var symdiff = {};
        this.dup(diff1, symdiff);
        this.dup(diff2, symdiff);
        return symdiff;
    }

    this.size = function(s) {
        var size = 0;
        for (var key in s) {
            if (s.hasOwnProperty(key)) {
                size++;
            }
        }

        return size;
    }

    this.fromKeys = function(o) {
        var s = this.makeSet();
        var keys = [];
        for (var i in o) {
            if (o.hasOwnProperty(i)) {
                Set.add(s, i);
            }
        }

        return s;
    }
}