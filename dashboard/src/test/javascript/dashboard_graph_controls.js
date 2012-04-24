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

describe('The graph controls', function () {
    it('should be able to build collector urls', function() {
        var url = buildHostSampleUrl(['hostA.company.com'], 'JVM', 'heapUsed', new Date('Sat, 21 Apr 2012 16:41 GMT'), new Date('Tue, 24 Apr 2012 16:20 GMT'), 10);
        expect(url).toBe('/rest/1.0/host_samples?category_and_sample_kind=JVM,heapUsed&from=2012-04-21T16:41:00Z&to=2012-04-24T16:20:00Z&output_count=10&host=hostA.company.com');
    });

    it('should be able to build left-shifted urls', function() {
        var url = shiftLeftUrl(['hostA.company.com'], 'JVM', 'heapUsed', new Date('Sat, 21 Apr 2012 12:00 GMT'), new Date('Tue, 24 Apr 2012 12:00 GMT'), 10);
        // Time interval is 3 days, so shift left by 1.5 day
        expect(url).toBe('/rest/1.0/host_samples?category_and_sample_kind=JVM,heapUsed&from=2012-04-20T00:00:00Z&to=2012-04-23T00:00:00Z&output_count=10&host=hostA.company.com');
    });

    it('should be able to build right-shifted urls', function() {
        var url = shiftRightUrl(['hostA.company.com'], 'JVM', 'heapUsed', new Date('Sat, 21 Apr 2012 12:00 GMT'), new Date('Tue, 24 Apr 2012 12:00 GMT'), 10);
        // Time interval is 3 days, so shift right by 1.5 day
        expect(url).toBe('/rest/1.0/host_samples?category_and_sample_kind=JVM,heapUsed&from=2012-04-23T00:00:00Z&to=2012-04-26T00:00:00Z&output_count=10&host=hostA.company.com');
    });

    it('should be able to build zoomed-in urls', function() {
        var url = zoomInUrl(['hostA.company.com'], 'JVM', 'heapUsed', new Date('Sat, 21 Apr 2012 12:00 GMT'), new Date('Tue, 24 Apr 2012 12:00 GMT'), 10);
        // Time interval is 3 days, so shift by 18 hours
        expect(url).toBe('/rest/1.0/host_samples?category_and_sample_kind=JVM,heapUsed&from=2012-04-22T06:00:00Z&to=2012-04-23T18:00:00Z&output_count=10&host=hostA.company.com');
    });

    it('should be able to build zoomed-out urls', function() {
        var url = zoomOutUrl(['hostA.company.com'], 'JVM', 'heapUsed', new Date('Sat, 21 Apr 2012 12:00 GMT'), new Date('Tue, 24 Apr 2012 12:00 GMT'), 10);
        // Time interval is 3 days, so shift by 18 hours
        expect(url).toBe('/rest/1.0/host_samples?category_and_sample_kind=JVM,heapUsed&from=2012-04-20T18:00:00Z&to=2012-04-25T06:00:00Z&output_count=10&host=hostA.company.com');
    });
});