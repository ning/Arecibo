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

describe('The dashboard input form', function () {
    it('should accept valid input times', function() {
        var startTime = 'Wed, 11 Apr 2012 12:20';
        var endTime = 'Thu, 26 Apr 2012 20:44';
        expect(validateDatesInput(startTime, endTime)).toBeNull();
    });

    it('should accept valid input dates', function() {
        var startDate = 'Wed, 11 Apr 2012';
        var endDate = 'Thu, 12 Apr 2012';
        expect(validateDatesInput(startDate, endDate)).toBeNull();
    });

    it('should complain and not make a collector query if the start time is greater than or equal to the end time', function () {
        var startTime = 'Tue, 24 Apr 2012 08:21';
        var endTime = 'Tue, 24 Apr 2012 08:20';

        expect(validateDatesInput(startTime, startTime)).toBe('The start time is greater than or equal to the end time');
        expect(validateDatesInput(startTime, endTime)).toBe('The start time is greater than or equal to the end time');
    });

    it('should complain and not make a collector query if times are bogus', function() {
        var bogusStartTime = 'salut';
        var validStartTime = 'Fri, 4 May 2012 00:00';
        var bogusEndTime = 'kikoo';
        var validEndTime = 'Sat, 5 May 2012 00:00';

        expect(validateDatesInput(bogusStartTime, validEndTime)).toBe('Invalid start time');
        expect(validateDatesInput(validStartTime, bogusEndTime)).toBe('Invalid end time');
        // We fail fast hence the error message on start time only
        expect(validateDatesInput(bogusStartTime, bogusEndTime)).toBe('Invalid start time');
    });
});