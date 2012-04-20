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

// Main routine executed at page load time
function setupAreciboUI() {
    // UI setup (Ajax handlers, etc.)
    initializeUI();
    setupDateTimePickers();

    // Dashboard Configuration
    window.arecibo = {
        // initializeUI() must be called first for window.location.origin to be set
        uri: window.location.origin // e.g. 'http://127.0.0.1:8080'
    }

    // Retrieve user's last input and populate the input fields
    try {
        $("#samples_start").val(localStorage.getItem("arecibo_latest_samples_start_lookup"));
        $("#samples_end").val(localStorage.getItem("arecibo_latest_samples_end_lookup"));
        window.arecibo.hosts_selected = JSON.parse(localStorage.getItem("arecibo_latest_hosts"));
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

        errorMessage = validateDatesInput(samples_start_lookup, samples_end_lookup);
        if (errorMessage) {
            alert(errorMessage);
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
};

// Return null if the dates are valid, an error message otherwise
function validateDatesInput(samplesStartString, samplesEndString) {
    var samplesStartDate = null;
    var samplesEndDate = null;

    try {
        samplesStartDate = new Date(samplesStartString);
        samplesEndDate = new Date(samplesEndString);
    } catch (err) {
        return 'Invalid start and/or end time';
    }

    if (isNaN(samplesStartDate.getTime())) {
        return 'Invalid start time';
    } else if (isNaN(samplesEndDate.getTime())) {
        return 'Invalid end time';
    } else if (samplesStartDate === null || samplesEndDate === null) {
        return 'Please specify a time range';
    } else if (samplesStartDate.getTime() >= samplesEndDate.getTime()) {
        return 'The start time is greater than or equal to the end time';
    } else {
        return null;
    }
}

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
        onSelect: function(flag, node) {
            updateSampleKindsTree();
        },
        checkbox: true,
        selectMode: 3
    });

    // Add the nodes
    var selectedHostsSet = Set.makeSet(window.arecibo.hosts_selected, 'hostName');
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
                        hideCheckbox: false,
                        expand: false,
                        select: false
                });
            }

            var selected = Set.contains(selectedHostsSet, host.hostName);
            var childNode = children[host.coreType];

            childNode.addChild({
                title: host.hostName,
                hideCheckbox: false,
                icon: false,
                select: selected
            });

            // If at least one child node is selected, expand the father
            if (selected) {
                // Note! This needs to happen after the child is added to the father
                childNode.expand(true);
            } else {
                // If at least one child node is not selected, don't select the father
                // TODO Unfortunately, the following will deselect ALL children
                //childNode.select(false);
            }
        }
    }
}

// Find all selected hosts and build the associated query parameter for the dashboard
// This will also set window.arecibo.hosts_selected to a list of tuples (hostName, category)
function buildHostsParamsFromTree() {
    var uri = '';
    var tree = $("#hosts_tree").dynatree("getTree").getSelectedNodes();
    window.arecibo.hosts_selected = [];

    for (var i in tree) {
        var node = tree[i];
        if (node.hasSubSel) {
            continue;
        } else {
            if (window.arecibo.hosts_selected.length > 0) {
                uri += '&';
            }

            var hostName = node.data.title;
            var category = null;
            if (node.parent) {
                category = node.parent.data.title;
            }

            uri += 'host=' + node.data.title;
            window.arecibo.hosts_selected.push({hostName: hostName, category: category});
        }
    }

    return uri;
}

// Find all selected sample kinds and build the associated query parameter for the dashboard
// This will also set window.arecibo.sample_kinds_selected to the number of samples selected
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

    window.arecibo.sample_kinds_selected = sampleKindsNb;

    return uri;
}

// Refresh the sample kinds tree
// This is called when a host is (un)selected on the hosts tree. Selecting or unselecting
// another host in the same category does not refresh the sample kinds tree
function updateSampleKindsTree() {
    // Find all selected nodes and build the uri for the dashboard
    var uri = buildHostsParamsFromTree();
    if (!uri) {
        return false;
    }

    // Remember selected nodes for the next page load
    try {
        localStorage.setItem("arecibo_latest_hosts", JSON.stringify(window.arecibo.hosts_selected));
    } catch (e) { /* Ignore quota issues, non supoprted Browsers, etc. */ }

    // Verify if we need to update the sample kinds tree or not
    var categoriesSelected = Set.makeSet(window.arecibo.hosts_selected, 'category');
    if (Set.equals(categoriesSelected, window.arecibo.categories_selected)) {
        return false;
    } else {
        window.arecibo.categories_selected = categoriesSelected;
    }

    try {
        $("#sample_kinds_tree").dynatree("getRoot").removeChildren();
    } catch(e){
        // Ignore if the tree was empty
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
                hideCheckbox: true
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
    var hosts_url = buildHostsParamsFromTree();
    var sample_kinds_url = buildCategoryAndSampleKindParamsFromTree();

    var nb_samples = Math.round(screen.width / (window.arecibo.hosts_selected.length * window.arecibo.sample_kinds_selected));

    var uri = '/static/graph.html?' +
                hosts_url + '&' +
                sample_kinds_url + '&' +
                'from=' + ISODateString(from) + '&' +
                'to=' + ISODateString(to) + '&' +
                'output_count=' + nb_samples;

    return uri;
}

/*
 * Setup the datetime widgets for start/end
 */
function setupDateTimePickers() {
    $('#samples_start').datetimepicker({
        dateFormat: $.datepicker.RFC_2822,
        timeFormat: 'hh:mm',
        showTimezone: false,
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
        dateFormat: $.datepicker.RFC_2822,
        timeFormat: 'hh:mm',
        showTimezone: false,
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