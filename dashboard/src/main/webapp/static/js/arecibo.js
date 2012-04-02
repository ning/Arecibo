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
    setupDateTimePickers();

    // Retrieve user's last input and populate the input fields
    try {
        $("#samples_start").val(localStorage.getItem("arecibo_latest_samples_start_lookup"));
        $("#samples_end").val(localStorage.getItem("arecibo_latest_samples_end_lookup"));
    } catch (e) { /* Ignore quota issues, non supoprted Browsers, etc. */ }

    // Setup the Graph button
    $("#crunch").click(function (event) {
        // Store locally the latest search
        var samples_start_lookup = $("#samples_start").val();
        var samples_end_lookup = $("#samples_end").val();
        try {
            localStorage.setItem("arecibo_latest_samples_start_lookup", samples_start_lookup);
            localStorage.setItem("arecibo_latest_samples_end_lookup", samples_end_lookup);
        } catch (e) { /* Ignore quota issues, non supoprted Browsers, etc. */ }

        if (!samples_start_lookup || !samples_end_lookup) {
            alert("Please specify a time range");
        } else {
            window.location = buildGraphURL();
        }
        // Don't refresh the page
        event.preventDefault();
    });

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
                hideCheckbox: false,
                icon: false
            });
        }
    }
}

function buildHostsParamsFromTree() {
    var uri = '';
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

    return uri;
}

function buildCategoryAndSampleKindParamsFromTree() {
    var uri = '';
    var tree = $("#sample_kinds_tree").dynatree("getTree").getSelectedNodes();
    var sampleKindsNb = 0;

    for (var i in tree) {
        var node = tree[i];
        if (node.hasSubSel) {
            continue;
        } else {
            if (sampleKindsNb > 0) {
                uri += '&';
            }

            uri += 'category_and_sample_kind=';
            var parent = node.getParent();
            if (parent != null) {
                uri += parent.data.title + ',';
            }

            uri += node.data.title;
            sampleKindsNb++;
        }
    }

    return uri;
}

function updateSampleKindsTree() {
    var uri = buildHostsParamsFromTree();

    try {
        $("#sample_kinds_tree").dynatree("getRoot").removeChildren();
    } catch(e){
        // Ignore if the tree was empty
    }

    if (!uri) {
        return false;
    }

    callArecibo('/rest/1.0/sample_kinds?' + uri, 'populateSampleKindsTree');
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
                hideCheckbox: false,
                icon: false
            });
        }
    }
}

function buildGraphURL() {
    var from = new Date($("#samples_start").val());
    var to = new Date($("#samples_end").val());
    var uri = '/static/graph.html?' +
                buildHostsParamsFromTree() + '&' +
                buildCategoryAndSampleKindParamsFromTree() + '&' +
                'from=' + ISODateString(from) + '&' +
                'to=' + ISODateString(to);

    return uri;
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