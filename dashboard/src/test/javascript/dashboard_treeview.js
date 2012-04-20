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

describe('The hosts checkbox tree', function () {
    var hostCategories;
    var hosts;
    var sampleKindsTreeRefreshed;

    beforeEach(function() {
        hostCategories = [];
        hosts = [];
        sampleKindsTreeRefreshed = false;

        // Mock the dynatree
        var fakeNode = {
            onSelect: function() {
                sampleKindsTreeRefreshed = true;
            },

            addChild: function(host) {
                hosts.push(host);
            }
        }
        var fakeDynatree = {
            onSelect: function() {
                sampleKindsTreeRefreshed = true;
            },

            addChild: function(categoryNode) {
                hostCategories.push(categoryNode);
                return fakeNode;
            }
        };
        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should sort hosts categories alphabetically', function() {
        var hostData = [
            {
                coreType: 'proxy/b',
                hostName: 'hostA.company.com'
            },
            {
                coreType: 'proxy/a',
                hostName: 'hostA.company.com'
            }
        ];
        populateHostsTree(hostData);

        expect(hostCategories[0].title).toEqual('proxy/a');
        expect(hostCategories[1].title).toEqual('proxy/b');
        expect(hostCategories.length).toEqual(2);

        expect(hosts[0].title).toEqual('hostA.company.com');
        expect(hosts[1].title).toEqual('hostA.company.com');
        expect(hosts.length).toEqual(2);
    });

    it('should sort hosts alphabetically', function() {
        var hostData = [
            {
                coreType: 'proxy',
                hostName: 'hostB.company.com'
            },
            {
                coreType: 'proxy',
                hostName: 'hostA.company.com'
            }
        ];
        populateHostsTree(hostData);

        expect(hostCategories[0].title).toEqual('proxy');
        expect(hostCategories.length).toEqual(1);

        expect(hosts[0].title).toEqual('hostA.company.com');
        expect(hosts[1].title).toEqual('hostB.company.com');
        expect(hosts.length).toEqual(2);
    });

    it('should have checkboxes for both categories and hosts', function() {
        var hostData = [
            {
                coreType: 'proxy/a',
                hostName: 'hostA.company.com'
            },
            {
                coreType: 'proxy/b',
                hostName: 'hostB.company.com'
            }
        ];
        populateHostsTree(hostData);

        expect(hostCategories[0].title).toEqual('proxy/a');
        expect(hostCategories[0].isFolder).toBeTruthy();
        expect(hostCategories[0].icon).toBeFalsy();
        expect(hostCategories[0].hideCheckbox).toBeFalsy();

        expect(hostCategories[1].title).toEqual('proxy/b');
        expect(hostCategories[1].isFolder).toBeTruthy();
        expect(hostCategories[1].icon).toBeFalsy();
        expect(hostCategories[1].hideCheckbox).toBeFalsy();

        expect(hostCategories.length).toEqual(2);

        expect(hosts[0].title).toEqual('hostA.company.com');
        expect(hosts[0].isFolder).toBeUndefined();
        expect(hosts[0].icon).toBeFalsy();
        expect(hosts[0].hideCheckbox).toBeFalsy();

        expect(hosts[1].title).toEqual('hostB.company.com');
        expect(hosts[1].isFolder).toBeUndefined();
        expect(hosts[1].icon).toBeFalsy();
        expect(hosts[1].hideCheckbox).toBeFalsy();

        expect(hosts.length).toEqual(2);
    });
});

describe('The sample kinds checkbox tree', function () {
    var sampleCategories;
    var sampleKinds;

    beforeEach(function() {
        sampleCategories = [];
        sampleKinds = [];

        // Mock the dynatree
        var fakeNode = {
            addChild: function(sampleKindNode) {
                sampleKinds.push(sampleKindNode);
            }
        }
        var fakeDynatree = {
            addChild: function(categoryNode) {
                sampleCategories.push(categoryNode);
                return fakeNode;
            }
        };
        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should sort sample categories alphabetically', function() {
        var kinds = [
            {
                eventCategory: 'JVMOperatingSystemPerZone',
                sampleKinds: []
            },
            {
                eventCategory: 'JVMMemory',
                sampleKinds: []
            }
        ];
        populateSampleKindsTree(kinds);

        expect(sampleCategories[0].title).toEqual('JVMMemory');
        expect(sampleCategories[1].title).toEqual('JVMOperatingSystemPerZone');
        expect(sampleCategories.length).toEqual(2);
    });

    it('should sort sample kinds alphabetically', function() {
        var kinds = [
            {
                eventCategory: 'JVMOperatingSystemPerZone',
                sampleKinds: ['ProcessCpuTime', 'OpenFileDescriptorCount']
            },
            {
                eventCategory: 'JVMMemory',
                sampleKinds: ['heapUsed', 'heapMax', 'nonHeapUsed', 'nonHeapMax']
            }
        ];
        populateSampleKindsTree(kinds);

        expect(sampleCategories[0].title).toEqual('JVMMemory');
        expect(sampleCategories[1].title).toEqual('JVMOperatingSystemPerZone');
        expect(sampleCategories.length).toEqual(2);

        expect(sampleKinds[0].title).toEqual('heapMax');
        expect(sampleKinds[1].title).toEqual('heapUsed');
        expect(sampleKinds[2].title).toEqual('nonHeapMax');
        expect(sampleKinds[3].title).toEqual('nonHeapUsed');
        expect(sampleKinds[4].title).toEqual('OpenFileDescriptorCount');
        expect(sampleKinds[5].title).toEqual('ProcessCpuTime');
        expect(sampleKinds.length).toEqual(6);
    });

    it('should not have checkboxes at the sample category level', function() {
        var kinds = [
            {
                eventCategory: 'JVMMemory',
                sampleKinds: []
            },
            {
                eventCategory: 'JVMOperatingSystemPerZone',
                sampleKinds: []
            }
        ];
        populateSampleKindsTree(kinds);

        expect(sampleCategories[0].title).toEqual('JVMMemory');
        expect(sampleCategories[0].isFolder).toBeTruthy();
        expect(sampleCategories[0].icon).toBeFalsy();
        expect(sampleCategories[0].hideCheckbox).toBeTruthy();

        expect(sampleCategories[1].title).toEqual('JVMOperatingSystemPerZone');
        expect(sampleCategories[1].isFolder).toBeTruthy();
        expect(sampleCategories[1].icon).toBeFalsy();
        expect(sampleCategories[1].hideCheckbox).toBeTruthy();

        expect(sampleCategories.length).toEqual(2);
    });

    it('should have checkboxes at the sample kind level', function() {
        var kinds = [
            {
                eventCategory: 'JVMMemory',
                sampleKinds: ['heapMax', 'heapUsed', 'nonHeapMax', 'nonHeapUsed']
            },
            {
                eventCategory: 'JVMOperatingSystemPerZone',
                sampleKinds: ['OpenFileDescriptorCount', 'ProcessCpuTime']
            }
        ];
        populateSampleKindsTree(kinds);

        expect(sampleKinds[0].title).toEqual('heapMax');
        expect(sampleKinds[0].isFolder).toBeUndefined();
        expect(sampleKinds[0].icon).toBeFalsy();
        expect(sampleKinds[0].hideCheckbox).toBeFalsy();

        expect(sampleKinds[1].title).toEqual('heapUsed');
        expect(sampleKinds[1].isFolder).toBeUndefined();
        expect(sampleKinds[1].icon).toBeFalsy();
        expect(sampleKinds[1].hideCheckbox).toBeFalsy();

        expect(sampleKinds[2].title).toEqual('nonHeapMax');
        expect(sampleKinds[2].isFolder).toBeUndefined();
        expect(sampleKinds[2].icon).toBeFalsy();
        expect(sampleKinds[2].hideCheckbox).toBeFalsy();

        expect(sampleKinds[3].title).toEqual('nonHeapUsed');
        expect(sampleKinds[3].isFolder).toBeUndefined();
        expect(sampleKinds[3].icon).toBeFalsy();
        expect(sampleKinds[3].hideCheckbox).toBeFalsy();

        expect(sampleKinds[4].title).toEqual('OpenFileDescriptorCount');
        expect(sampleKinds[4].isFolder).toBeUndefined();
        expect(sampleKinds[4].icon).toBeFalsy();
        expect(sampleKinds[4].hideCheckbox).toBeFalsy();

        expect(sampleKinds[5].title).toEqual('ProcessCpuTime');
        expect(sampleKinds[5].isFolder).toBeUndefined();
        expect(sampleKinds[5].icon).toBeFalsy();
        expect(sampleKinds[5].hideCheckbox).toBeFalsy();

        expect(sampleKinds.length).toEqual(6);
    });
});