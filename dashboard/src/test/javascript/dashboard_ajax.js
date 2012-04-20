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

describe('The sample kinds update routine', function () {
    var areciboUri = 'the-machine';
    var hostUri = 'host=A';

    beforeEach(function() {
        window.arecibo = {
            uri: areciboUri
        };

        spyOn(window, 'buildHostsParamsFromTree').andCallFake(function() {
            window.arecibo.hosts_selected = [{hostName: 'hostA.company.com', category: 'proxy/a'}];
            return hostUri;
        });
        spyOn($.fn, 'dynatree');
        spyOn($, 'ajax');
    });

    it('refresh the sample kinds tree when a new host is selected', function() {
        expect(Set.size(window.arecibo.categories_selected)).toEqual(0);
        expect($.ajax.callCount).toEqual(0);

        updateSampleKindsTree();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);

        window.arecibo.hosts_selected.push({hostName: 'hostB.company.com', category: 'proxy/b'});
        hostUri = hostUri + '&host=B';
        window.buildHostsParamsFromTree.andReturn(hostUri);

        updateSampleKindsTree();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(2);
        expect($.ajax.callCount).toEqual(2);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);
    });

    it('selecting or unselecting another host in the same category should not refresh the sample kinds tree', function() {
        expect(Set.size(window.arecibo.categories_selected)).toEqual(0);
        expect($.ajax.callCount).toEqual(0);

        updateSampleKindsTree();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
        expect($.ajax.mostRecentCall.args[0]["url"]).toEqual(areciboUri + '/rest/1.0/sample_kinds?' + hostUri);

        updateSampleKindsTree();
        expect(Set.size(window.arecibo.categories_selected)).toEqual(1);
        expect($.ajax.callCount).toEqual(1);
    });
});