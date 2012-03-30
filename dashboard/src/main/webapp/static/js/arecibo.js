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

$(document).ready(function() {
    // Dashboard Configuration
    window.arecibo = {
        uri: window.location.origin // e.g. 'http://127.0.0.1:8080'
    }

    // UI setup (Ajax handlers, etc.)
    initializeUI();

    // Update hosts tree
    updateHostsTree();

    // Create en empty sample kinds tree as placeholder
    populateSampleKindsTree([]);
});

function updateHostsTree() {
    callArecibo('/rest/1.0/hosts', 'populateHostsTree');
}

function populateHostsTree(hosts) {
    // Order by core type and hostName alphabetically
    hosts.sort(function(a, b) {
            var x = a['coreType']; var y = b['coreType'];
            if (x == y) {
                x = a['hostName']; y = b['hostName'];
            }

            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });

    // Create the tree
    $("#hosts_tree").dynatree({
        onSelect: function(node) {
            updateSampleKindsTree();
        },
        //persistent: true,
        checkbox: true,
        selectMode: 3
    });

    // Add the nodes
    var rootNode = $("#hosts_tree").dynatree("getRoot");
    var children = {};
    for (var i in hosts) {
        var host = hosts[i];

        if (!host.coreType) {
            //rootNode.addChild({
            //    title: host.hostName
            //});
        }
        else {
            if (children[host.coreType] === undefined) {
                children[host.coreType] = rootNode.addChild({
                        title: host.coreType,
                        isFolder: true,
                        icon: false,
                        hideCheckbox: false
                });
            }

            var childNode = children[host.coreType];
            childNode.addChild({
                title: host.hostName,
                hideCheckbox: false
            });
        }
    }
}

function updateSampleKindsTree() {
    var uri = '/rest/1.0/sample_kinds?';
    var tree = $("#hosts_tree").dynatree("getTree").getSelectedNodes();
    var hostsNb = 0;

    for (var i in tree) {
        var node = tree[i];
        if (node.hasSubSel) {
            continue;
        } else {
            if (hostsNb > 0) {
                uri += '&';
            }

            uri += 'host=' + node.data.title;
            hostsNb++;
        }
    }

    try {
        $("#sample_kinds_tree").dynatree("getRoot").removeChildren();
    } catch(e){
        // Ignore if the tree was empty
    }

    if (hostsNb == 0) {
        return false;
    }

    callArecibo(uri, 'populateSampleKindsTree');
    return false;
}

function populateSampleKindsTree(kinds) {
    // Order by eventCategory alphabetically
    kinds.sort(function(a, b) {
            var x = a['eventCategory']; var y = b['eventCategory'];
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });

    $("#sample_kinds_tree").dynatree({
        checkbox: true,
        selectMode: 3
    });

    var rootNode = $("#sample_kinds_tree").dynatree("getRoot");
    var children = {};
    for (var i in kinds) {
        var category = kinds[i];
        var childNode = rootNode.addChild({
                title: category.eventCategory,
                isFolder: true,
                icon: false,
                hideCheckbox: false
        });

        var sampleKinds = category.sampleKinds;
        // Order by sample kind alphabetically
        sampleKinds.sort();
        for (var j in sampleKinds) {
            var kind = sampleKinds[j];
            childNode.addChild({
                title: kind,
                hideCheckbox: false
            });
        }
    }
}

function callArecibo(uri, callback, opts) {
    var ajax_opts = {
        url: window.arecibo['uri'] + uri,
        dataType: "jsonp",
        cache : false,
        jsonp : "callback",
        jsonpCallback: callback,
        // Hacky error handling for JSONP requests
        timeout : 20000
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

    setupDateTimePickers();
}

/*
 * Setup the datetime widgets for start/end
 */
function setupDateTimePickers() {
    $('#samples_start').datetimepicker({
        hourGrid: 4,
        minuteGrid: 10,
        onClose: function(dateText, inst) {
            var endDateTextBox = $('#samples_end');
            if (endDateTextBox.val() != '') {
                var testStartDate = new Date(dateText);
                var testEndDate = new Date(endDateTextBox.val());
                if (testStartDate > testEndDate)
                    endDateTextBox.val(dateText);
            }
            else {
                endDateTextBox.val(dateText);
            }
        },
        onSelect: function (selectedDateTime){
            var start = $(this).datetimepicker('getDate');
            $('#samples_end').datetimepicker('option', 'minDate', new Date(start.getTime()));
        }
    });

    $('#samples_end').datetimepicker({
        hourGrid: 4,
        minuteGrid: 10,
        onClose: function(dateText, inst) {
            var startDateTextBox = $('#samples_start');
            if (startDateTextBox.val() != '') {
                var testStartDate = new Date(startDateTextBox.val());
                var testEndDate = new Date(dateText);
                if (testStartDate > testEndDate)
                    startDateTextBox.val(dateText);
            }
            else {
                startDateTextBox.val(dateText);
            }
        },
        onSelect: function (selectedDateTime){
            var end = $(this).datetimepicker('getDate');
            $('#samples_start').datetimepicker('option', 'maxDate', new Date(end.getTime()));
        }
    });
}