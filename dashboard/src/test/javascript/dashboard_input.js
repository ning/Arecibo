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

describe('The dashboard input form validator', function() {
    var broken = 'BROKEN!';

    it('should accept valid entries', function() {
        spyOn(window, 'validateHostsInput').andReturn(null);
        spyOn(window, 'validateSampleKindsInput').andReturn(null);
        spyOn(window, 'validateDatesInput').andReturn(null);

        expect(validateInput()).toBeNull();
    });

    it('should reject an invalid hosts selection', function() {
        spyOn(window, 'validateHostsInput').andReturn(broken);
        spyOn(window, 'validateSampleKindsInput').andReturn(null);
        spyOn(window, 'validateDatesInput').andReturn(null);

        expect(validateInput()).toBe(broken);
    });

    it('should reject an invalid sample kinds selection', function() {
        spyOn(window, 'validateHostsInput').andReturn(null);
        spyOn(window, 'validateSampleKindsInput').andReturn(broken);
        spyOn(window, 'validateDatesInput').andReturn(null);

        expect(validateInput()).toBe(broken);
    });

    it('should reject an invalid dates selection', function() {
        spyOn(window, 'validateHostsInput').andReturn(null);
        spyOn(window, 'validateSampleKindsInput').andReturn(null);
        spyOn(window, 'validateDatesInput').andReturn(broken);

        expect(validateInput()).toBe(broken);
    });
});

describe('The dashboard input form host validator', function() {
    var hosts;

    beforeEach(function() {
        // Mock the dynatree
        var fakeDynatree = {
            getSelectedNodes: function() {
                return hosts;
            }
        };

        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should accept a single host selection', function() {
        hosts = ['hostA.company.com'];
        expect(validateHostsInput()).toBeNull;
    });

    it('should accept a multiple hosts selection', function() {
        hosts = ['hostA.company.com', 'hostB.company.com'];
        expect(validateHostsInput()).toBeNull;
    });

    it('should complain and not make a collector query if no host is selected', function() {
        hosts = [];
        expect(validateHostsInput()).toBe('No host selected');

        hosts = null;
        expect(validateHostsInput()).toBe('No host selected');

        hosts = undefined;
        expect(validateHostsInput()).toBe('No host selected');
    });
});

describe('The dashboard input form sample kind validator', function() {
    var sampleKinds;

    beforeEach(function() {
        // Mock the dynatree
        var fakeDynatree = {
            getSelectedNodes: function() {
                return sampleKinds;
            }
        };

        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should accept a single sample kind selection', function() {
        sampleKinds = ['heapMax'];
        expect(validateSampleKindsInput()).toBeNull;
    });

    it('should accept a multiple sample kinds selection', function() {
        sampleKinds = ['heapMax', 'heapUsed'];
        expect(validateSampleKindsInput()).toBeNull;
    });

    it('should complain and not make a collector query if no sample kind is selected', function() {
        sampleKinds = [];
        expect(validateSampleKindsInput()).toBe('No sample kind selected');

        sampleKinds = null;
        expect(validateSampleKindsInput()).toBe('No sample kind selected');

        sampleKinds = undefined;
        expect(validateSampleKindsInput()).toBe('No sample kind selected');
    });
});

describe('The dashboard input form datetime validator', function() {
    var firstCall = true;
    var startTime;
    var endTime;

    beforeEach(function() {
        spyOn($.fn, 'val').andCallFake(function() {
            if (firstCall) {
                firstCall = false;
                return startTime;
            } else {
                // reset for re-use
                firstCall = true;
                return endTime;
            }
        });
    });

    it('should accept valid input times', function() {
        startTime = 'Wed, 11 Apr 2012 12:20';
        endTime = 'Thu, 26 Apr 2012 20:44';
        expect(validateDatesInput()).toBeNull();
    });

    it('should accept valid input dates', function() {
        startTime = 'Wed, 11 Apr 2012';
        endTime = 'Thu, 12 Apr 2012';
        expect(validateDatesInput()).toBeNull();
    });

    it('should complain and not make a collector query if the start time is greater than or equal to the end time', function () {
        startTime = 'Tue, 24 Apr 2012 08:21';
        endTime = 'Tue, 24 Apr 2012 08:20';

        expect(validateDatesInput()).toBe('The start time is greater than or equal to the end time');
        expect(validateDatesInput()).toBe('The start time is greater than or equal to the end time');
    });

    it('should complain and not make a collector query if times are bogus', function() {
        startTime = 'salut';
        endTime = 'Sat, 5 May 2012 00:00';
        expect(validateDatesInput()).toBe('Invalid start time');

        startTime = 'Fri, 4 May 2012 00:00';
        endTime = 'kikoo';
        expect(validateDatesInput()).toBe('Invalid end time');

        startTime = 'salut';
        endTime = 'kikoo';
        // We fail fast hence the error message on start time only
        expect(validateDatesInput()).toBe('Invalid start time');
    });
});