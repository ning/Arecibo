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

describe('The hosts uri builder', function () {
    var hosts;

    beforeEach(function() {
        window.arecibo = {};
        hosts = [];

        // Mock the dynatree
        var fakeDynatree = {
            getSelectedNodes: function() {
                return hosts;
            }
        };
        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should set the global window.arecibo.hosts_selected variable', function() {
        hosts.push(
            {
                parent: {
                    data: {
                        title: 'proxy/a'
                    }
                },
                data: {
                    title: 'hostA.company.com'
                }
            },
            {
                parent: {
                    data: {
                        title: 'proxy/b'
                    }
                },
                data: {
                    title: 'hostB.company.com'
                }
            }
        );
        var uri = buildHostsParamsFromTree();

        expect(uri).toEqual('host=hostA.company.com&host=hostB.company.com');

        expect(window.arecibo.hosts_selected[0]).toEqual({hostName: 'hostA.company.com', category: 'proxy/a'});
        expect(window.arecibo.hosts_selected[1]).toEqual({hostName: 'hostB.company.com', category: 'proxy/b'});
        expect(window.arecibo.hosts_selected.length).toEqual(2);
    });
});

describe('The sample kinds uri builder', function () {
    var sampleKinds;

    beforeEach(function() {
        window.arecibo = {};
        sampleKinds = [];

        // Mock the dynatree
        var fakeDynatree = {
            getSelectedNodes: function() {
                return sampleKinds;
            }
        };
        spyOn($.fn, 'dynatree').andReturn(fakeDynatree);
    });

    it('should set the global window.arecibo.sample_kinds_selected variable', function() {
        sampleKinds.push(
            {
                parent: {
                    data: {
                        title: 'JVMMemory'
                    }
                },
                data: {
                    title: 'heapUsed'
                }
            },
            {
                parent: {
                    data: {
                        title: 'JVMOperatingSystemPerZone'
                    }
                },
                data: {
                    title: 'ProcessCpuTime'
                }
            }
        );
        var uri = buildCategoryAndSampleKindParamsFromTree();

        expect(uri).toEqual('category_and_sample_kind=JVMMemory,heapUsed&category_and_sample_kind=JVMOperatingSystemPerZone,ProcessCpuTime');

        expect(window.arecibo.sample_kinds_selected[0]).toEqual({sampleKind: 'heapUsed', sampleCategory: 'JVMMemory'});
        expect(window.arecibo.sample_kinds_selected[1]).toEqual({sampleKind: 'ProcessCpuTime', sampleCategory: 'JVMOperatingSystemPerZone'});
        expect(window.arecibo.sample_kinds_selected.length).toEqual(2);
    });
});

describe('The hostsSelected callback routine', function () {
    var items;
    var areciboUri = 'the-machine';
    var hostUri = 'host=A';

    beforeEach(function() {
        window.arecibo = {
            uri: areciboUri
        };

        // Mock the local storage
        if (window.localStorage === undefined) {
            // Not available at runtime?
            window.localStorage = {
                setItem: function(key, value) {}
            };
        }
        items = {};
        spyOn(localStorage, 'setItem').andCallFake(function(key, value) {
            items[key] = value;
        });
        spyOn(window, 'buildHostsParamsFromTree').andCallFake(function() {
            window.arecibo.hosts_selected = [{hostName: 'hostA.company.com', category: 'proxy/a'}];
            return hostUri;
        });
        spyOn($.fn, 'dynatree');
        spyOn($, 'ajax');
    });

    it('should refresh the sample kinds tree when a new host is selected', function() {
        expect(Set.size(window.arecibo.categories_selected)).toEqual(0);
        expect($.ajax.callCount).toEqual(0);

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);

        window.arecibo.hosts_selected.push({hostName: 'hostB.company.com', category: 'proxy/b'});
        hostUri = hostUri + '&host=B';
        window.buildHostsParamsFromTree.andReturn(hostUri);

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(2);
        expect($.ajax.callCount).toEqual(2);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);
    });

    it('should not refresh the sample kinds tree when selecting or unselecting another host in the same category', function() {
        expect(Set.size(window.arecibo.categories_selected)).toEqual(0);
        expect($.ajax.callCount).toEqual(0);

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
    });

    it('should remember the latest hosts selected', function() {
        expect(Set.size(window.arecibo.categories_selected)).toEqual(0);
        expect(Set.size(window.arecibo.hosts_selected)).toEqual(0);
        expect(items['arecibo_latest_hosts']).toBeUndefined();

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect(Set.size(window.arecibo.hosts_selected)).toEqual(1);
        expect(items['arecibo_latest_hosts']).toBe(JSON.stringify(window.arecibo.hosts_selected));

        window.arecibo.hosts_selected.push({hostName: 'hostB.company.com', category: 'proxy/b'});
        hostUri = hostUri + '&host=B';
        window.buildHostsParamsFromTree.andReturn(hostUri);

        hostsSelected();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(2);
        expect(Set.size(window.arecibo.hosts_selected)).toEqual(2);
        expect(items['arecibo_latest_hosts']).toBe(JSON.stringify(window.arecibo.hosts_selected));
    });
});

describe('The sampleKindsSelected callback routine', function () {
    var items;
    var areciboUri = 'the-machine';
    var sampleKindsUri = 'sampleKinds=A,B,C';

    beforeEach(function() {
        window.arecibo = {
            uri: areciboUri
        };

        // Mock the local storage
        if (window.localStorage === undefined) {
            // Not available at runtime?
            window.localStorage = {
                setItem: function(key, value) {}
            };
        }
        items = {};
        spyOn(localStorage, 'setItem').andCallFake(function(key, value) {
            items[key] = value;
        });
        spyOn(window, 'buildCategoryAndSampleKindParamsFromTree').andCallFake(function() {
            window.arecibo.sample_kinds_selected = [{sampleKind: 'heapUsed', sampleCategory: 'JVMMemory'}];
            return sampleKindsUri;
        });
        spyOn($.fn, 'dynatree');
        spyOn($, 'ajax');
    });

    it('should remember the latest sample kinds selected', function() {
        expect(Set.size(window.arecibo.sample_kinds_selected)).toEqual(0);
        expect(items['arecibo_latest_sample_kinds']).toBeUndefined();

        sampleKindsSelected();
        expect(Set.size(window.arecibo.sample_kinds_selected)).toEqual(1);
        expect(items['arecibo_latest_sample_kinds']).toBe(JSON.stringify(window.arecibo.sample_kinds_selected));

        window.arecibo.sample_kinds_selected.push({sampleKind: 'ProcessCpuTime', sampleCategory: 'JVMOperatingSystemPerZone'});
        hostUri = sampleKindsUri + '&sampleKinds=D,E,F';
        window.buildCategoryAndSampleKindParamsFromTree.andReturn(hostUri);

        sampleKindsSelected();
        expect(Set.size(window.arecibo.sample_kinds_selected)).toEqual(2);
        expect(items['arecibo_latest_sample_kinds']).toBe(JSON.stringify(window.arecibo.sample_kinds_selected));
    });
});