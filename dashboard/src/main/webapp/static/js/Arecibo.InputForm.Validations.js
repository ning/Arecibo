/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the 'License'); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

Arecibo.namespace('Arecibo.InputForm.Validations');

Arecibo.InputForm.Validations = function() {
    var that = this;

    // Return null if the selection is valid, an error message otherwise
    this.validateInput = function(hosts, kinds, samplesStartSelector, samplesEndSelector) {
        var errorMessage = that.validateHostsInput(hosts);
        if (errorMessage) {
            return errorMessage;
        }

        errorMessage = that.validateSampleKindsInput(kinds);
        if (errorMessage) {
            return errorMessage;
        }

        errorMessage = that.validateDatesInput(samplesStartSelector, samplesEndSelector);
        if (errorMessage) {
            return errorMessage;
        }

        return null;
    };

    /**
     * Return null if the hosts selection is valid, an error message otherwise
     *
     * @visibleForTesting
     */
    this.validateHostsInput = function(hosts) {
        if (Set.elements(hosts).length == 0) {
            return 'No host selected';
        } else {
            return null;
        }
    };

    /**
     * Return null if the sample kinds selection is valid, an error message otherwise
     *
     * @visibleForTesting
     */
    this.validateSampleKindsInput = function(kinds) {
        if (Set.elements(kinds).length == 0) {
            return 'No sample kind selected';
        } else {
            return null;
        }
    };

    /**
     * Return null if the dates are valid, an error message otherwise
     *
     * @visibleForTesting
     */
    this.validateDatesInput = function(samplesStartSelector, samplesEndSelector) {
        var samplesStartDate = null;
        var samplesEndDate = null;

        try {
            samplesStartDate = new Date(samplesStartSelector.val());
            samplesEndDate = new Date(samplesEndSelector.val());
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
    };
}