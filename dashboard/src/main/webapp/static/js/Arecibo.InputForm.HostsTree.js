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

Arecibo.namespace('Arecibo.InputForm.HostsTree');

Arecibo.InputForm.HostsTree = function(sampleKindsTree) {
    // Reference to currently selected hosts (set of hostnames)
    var selectedHosts = Arecibo.InputForm.LocalStore.getLatestHostsSelected();
    // Reference to the tree root node
    var rootNode = null;
    // Reference to the jQuery selector for the hosts tree
    var treeSelector = $("#hosts_tree");
    var that = this;

    /**
     * Return the set of hostnames currently selected
     */
    this.getSelectedHosts = function() {
        return selectedHosts;
    }

    /**
     * Call the dashboard core to get the list of hosts and populate the tree
     */
    this.fetchHostsAndPopulateTree = function() {
        callArecibo('/rest/1.0/hosts', 'populateHostsTree', {
            beforeSend: function(xhr) {
                var etag = Arecibo.InputForm.LocalStore.getHostsEtag();
                if (etag) {
                    xhr.setRequestHeader('If-None-Match', etag);
                }
            },
            success: function(data, textStatus, xhr) {
                if (xhr.status == 304) {
                    data = Arecibo.InputForm.LocalStore.getHosts();
                } else {
                    // The hosts have been updated
                    data = JSON.parse(data);
                    etag = xhr.getResponseHeader('Etag');
                    Arecibo.InputForm.LocalStore.setHosts(data);
                    Arecibo.InputForm.LocalStore.setHostsEtag(etag);
                }
                that.populateHostsTree(data);
            }
        });
    };

    /**
     * Given a list of hosts, create the associated treeview
     *
     * @visibleForTesting
     */
    this.populateHostsTree = function(hosts) {
        // Create the tree
        createRootNode();

        // Order core types alphabetically
        var coreTypes = Set.fromKeys(hosts);
        coreTypes = Set.elements(coreTypes).sort();

        for (var i in coreTypes) {
            var coreType = coreTypes[i];
            var hostNames = hosts[coreType];

            if (!coreType) {
                // TODO - what should we do here?
                //addHost(rootNode, hostName, selected);
            }
            else {
                addCoreTypeGroup(coreType, hostNames);
            }
        }

        // Update the summary box for hosts
        updateHostsSelectedSummary();

        // Signal that the sample kinds tree can be loaded
        $(document).trigger('hostsTree:loaded');
    };

    /**
     * Return the host portion of the query parameter for the dashboard
     */
    this.toURI = function() {
        var first = true;
        var uri = '';

        var hosts = Set.elements(selectedHosts);
        for (var i in hosts) {
            var hostName = hosts[i];
            if (first) {
                first = false;
            } else {
                uri += '&';
            }

            uri += 'host=' + hostName;
        }

        return uri;
    };

    /**
     * Callback when a node is (de)selected
     */
    function nodeSelected(node) {
        var hosts = [];

        // If a group of hosts is selected
        if (node.hasChildren()) {
            for (var i in node.childList) {
                var child = node.childList[i];
                hosts = hosts.concat(nodeSelected(child));
            }
        } else {
            hosts.push(node.data.title);
        }

        return hosts;
    };

    /**
     * Final callback after a node is (de)selected
     */
    function hostsSelected(selected, hosts) {
        // Update the internal list of selected hosts
        for (var i in hosts) {
            var hostName = hosts[i];
            if (selected) {
                that.addSelectedHost(hostName);
            } else {
                removeSelectedHost(hostName);
            }
        }

        // Verify if we need to update the tree
        // TODO
        if (true) {
            sampleKindsTree.populateSampleKindsTree(selected, hosts);
        }
    };

    /**
     * Select a host
     *
     * @visibleForTesting
     */
    this.addSelectedHost = function(hostName) {
        Set.add(selectedHosts, hostName);
        selectedHostsUpdated();
    };

    /**
     * Deselect a host
     */
    removeSelectedHost = function(hostName) {
        Set.remove(selectedHosts, hostName);
        selectedHostsUpdated();
    };

    /**
     * Update internal states whenever the list of selected hosts is updated
     */
    function selectedHostsUpdated() {
        // Remember selected nodes for the next page load
        Arecibo.InputForm.LocalStore.setLatestHostsSelected(selectedHosts);

        // Update the summary box
        updateHostsSelectedSummary();
    }

    /**
     * Update the hosts list summary
     */
    function updateHostsSelectedSummary() {
        $('#hosts_summary_list').html('');

        var hosts = Set.elements(selectedHosts);
        for (var i in hosts) {
            var hostName = hosts[i];
            var hostItem = $('<li></li>').html(hostName);
            $('#hosts_summary_list').append(hostItem);
        }
    }

    /**
     * Create the top-level root node
     */
    function createRootNode() {
        treeSelector.dynatree({
            onSelect: function(flag, node) {
                // isLazy if it's a top-level node
                if (node.data.isLazy) {
                    // Load the hosts
                    onLazyReadCallback(node);
                    // Select all children
                    for (var i in node.childList) {
                        node.childList[i].select(flag);
                    }
                    node.expand(true);
                };

                var hosts = nodeSelected(node);
                hostsSelected(flag, hosts);
            },
            onLazyRead: function(node) {
                onLazyReadCallback(node);
            },
            checkbox: true,
            selectMode: 3
        });

        // Cache the root node
        rootNode = treeSelector.dynatree('getRoot');
    };

    function onLazyReadCallback(node) {
        if (node.data.areciboHosts && !node.hasChildren()) {
            addHostsForGroup(node, node.data.areciboHosts);
        }
    };

    /**
     * Add a node for a group of hosts
     */
    function addCoreTypeGroup(coreType, hosts) {
        var opts = {
                title: coreType,
                isFolder: true,
                icon: false,
                hideCheckbox: false,
                expand: false,
                select: false
        };

        var child;
        var childrenSet = Set.makeSet(hosts, 'hostName');
        if (Set.size(Set.inter(selectedHosts, childrenSet)) > 0) {
            // At least one child is selected - don't make this subtree lazy
            child = rootNode.addChild(opts);
            addHostsForGroup(child, hosts);
        } else {
            // For performance reasons, we don't add the children yet but use a custom
            // attribute to cache them. This will be used in the onLazyRead() callback.
            opts['isLazy'] = true;
            opts['areciboHosts'] = hosts;
            child = rootNode.addChild(opts);
        }

        return child;
    };

    /**
     * Add the children hosts for a core type group
     */
    function addHostsForGroup(node, hosts) {
        hosts = sort(hosts, 'hostName');
        for (var i in hosts) {
            var hostName = hosts[i].hostName;
            var selected = Set.contains(selectedHosts, hostName);
            addHost(node, hostName, selected);
        }
    };

    /**
     * Add a node for a single host
     */
    function addHost(parentNode, hostName, selected) {
        parentNode.addChild({
            title: hostName,
            hideCheckbox: false,
            icon: false,
            select: selected
        });

        // If at least one child node is selected, expand the father
        if (selected) {
            // Note! This needs to happen after the child is added to the father
            parentNode.expand(true);
        } else {
            // If at least one child node is not selected, don't select the father
            // TODO Unfortunately, the following will deselect ALL children
            //parentNode.select(false);
        }
    };
};