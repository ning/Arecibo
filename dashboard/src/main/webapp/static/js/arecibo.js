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

window.Arecibo = {
   namespace: function(namespace, obj) {
       var parts = namespace.split('.');
       var parent = window.Arecibo;

       for (var i = 1, length = parts.length; i < length; i++) {
           currentPart = parts[i];
           parent[currentPart] = parent[currentPart] || {};
           parent = parent[currentPart];
       }

       return parent;
   },

   keys: function(objj) {
       var keys = [];
       for (var key in obj) keys.push(key);
       return keys;
   }
};

// Main routine executed at page load time
function setupAreciboUI() {
    // UI setup (Ajax handlers, etc.)
    initializeUI();
    setupDateTimePickers();

    // Retrieve user's last input and populate the input fields
    try {
        samplesStartSelector().val(localStorage.getItem("arecibo_latest_samples_start_lookup"));
        samplesEndSelector().val(localStorage.getItem("arecibo_latest_samples_end_lookup"));
        window.arecibo.hosts_selected = JSON.parse(localStorage.getItem("arecibo_latest_hosts"));
        window.arecibo.sample_kinds_selected = JSON.parse(localStorage.getItem("arecibo_latest_sample_kinds"));
    } catch (e) { /* Ignore quota issues, non supported Browsers, etc. */ }

    window.arecibo.sampleKindsTree = new Arecibo.InputForm.SampleKindsTree();
    window.arecibo.hostsTree = new Arecibo.InputForm.HostsTree(window.arecibo.sampleKindsTree);

    // Update the hosts tree (and the summary box for hosts if any is selected)
    window.arecibo.hostsTree.fetchHostsAndPopulateTree();
    // Setup the sample kinds tree when loaded
    $(document).live('hostsTree:loaded', function() {
        window.arecibo.sampleKindsTree.fetchSampleKindsAndInitTree();
    });

    // Setup the Graph button
    $("#crunch").click(function (event) {
        // Store locally the latest search
        var samples_start_lookup = samplesStartSelector().val();
        var samples_end_lookup = samplesEndSelector().val();
        try {
            localStorage.setItem("arecibo_latest_samples_start_lookup", samples_start_lookup);
            localStorage.setItem("arecibo_latest_samples_end_lookup", samples_end_lookup);
        } catch (e) { /* Ignore quota issues, non supported Browsers, etc. */ }

        var errorMessage = new Arecibo.InputForm.Validations()
                                    .validateInput(window.arecibo.hostsTree.getSelectedHosts(),
                                                   window.arecibo.sampleKindsTree.getSelectedSampleKinds(),
                                                   samplesStartSelector(),
                                                   samplesEndSelector());
        if (errorMessage) {
            alert(errorMessage);
        } else {
            window.location = buildGraphURL();
        }
        // Don't refresh the page
        event.preventDefault();
    });
};

function buildGraphURL() {
    var from = new Date(samplesStartSelector().val());
    var to = new Date(samplesEndSelector().val());
    var hosts_url = window.arecibo.hostsTree.toURI();
    var sample_kinds_url = window.arecibo.sampleKindsTree.toURI();

    var nb_samples = 500;

    var uri = '/static/graph.html?' +
                hosts_url + '&' +
                sample_kinds_url + '&' +
                'from=' + ISODateString(from) + '&' +
                'to=' + ISODateString(to) + '&' +
                'output_count=' + nb_samples;

    return uri;
}

// Return the selector for the samples start input
function samplesStartSelector() {
    return $('#samples_start');
}

// Return the selector for the samples end input
function samplesEndSelector() {
    return $('#samples_end');
}

/*
 * Setup the datetime widgets for start/end
 */
function setupDateTimePickers() {
    samplesStartSelector().datetimepicker({
        dateFormat: $.datepicker.RFC_2822,
        timeFormat: 'hh:mm',
        showTimezone: false,
        hourGrid: 4,
        minuteGrid: 10,
        onClose: function(dateText, inst) {
            var endDateTextBox = samplesEndSelector();
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
            samplesEndSelector().datetimepicker('option', 'minDate', new Date(start.getTime()));
        }
    });

    samplesEndSelector().datetimepicker({
        dateFormat: $.datepicker.RFC_2822,
        timeFormat: 'hh:mm',
        showTimezone: false,
        hourGrid: 4,
        minuteGrid: 10,
        onClose: function(dateText, inst) {
            var startDateTextBox = samplesStartSelector();
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
            samplesStartSelector().datetimepicker('option', 'maxDate', new Date(end.getTime()));
        }
    });
}
