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

Arecibo.namespace('Arecibo.InputForm.SampleKindsTree');

Arecibo.InputForm.SampleKindsTree = function() {
    var allKinds;
    // Reference to currently selected sample kinds
    var selectedSampleKinds = Arecibo.InputForm.LocalStore.getLatestSampleKindsSelected();
    // Current groups drawn
    var groupsDrawn = Set.makeSet();
    // Current beans drawn
    var beansDrawn = Set.makeSet();
    // Reference to the tree root node
    var rootNode = null;
    // Reference to the jQuery selector for the sample kinds tree
    var treeSelector = $("#sample_kinds_tree");
    var that = this;

    /**
     * Returh the set of sample kinds currently selected
     */
    this.getSelectedSampleKinds = function() {
        return selectedSampleKinds;
    }

    /**
     * Call the dashboard core to get the list of sample kinds and populate the tree
     */
    this.fetchSampleKindsAndInitTree = function() {
        callArecibo('/rest/1.0/sample_kinds', 'populateSampleKindsTree', {
            beforeSend: function(xhr) {
                var etag = Arecibo.InputForm.LocalStore.getSampleKindsEtag();
                if (etag) {
                    xhr.setRequestHeader('If-None-Match', etag);
                }
            },
            success: function(data, textStatus, xhr) {
                if (xhr.status == 304) {
                    allKinds = Arecibo.InputForm.LocalStore.getSampleKinds();
                } else {
                    // The sample kinds have been updated
                    data = JSON.parse(data);

                    // Store them sorted
                    var groups = sort(data.groups, 'name');
                    var sampleKinds = sort2(data.sampleKinds, 'categoryAndSampleKinds', 'eventCategory');
                    allKinds = {groups: groups, sampleKinds: sampleKinds};
                    Arecibo.InputForm.LocalStore.setSampleKinds(allKinds);

                    etag = xhr.getResponseHeader('Etag');
                    Arecibo.InputForm.LocalStore.setSampleKindsEtag(etag);
                }
                createRootNode();

                // Signal that the sample kinds tree has been loaded
                $(document).trigger('sampleKindsTree:loaded');
            }
        });
    };

    function isSampleKindForHosts(kind, hosts) {
        for (var i in hosts) {
            if (kind.hosts.indexOf(hosts[i]) != -1) {
                return true;
            }
        }
        return false;
    };

    function shouldDrawGroup(groupKinds, kindsToDraw) {
        for (var i in groupKinds) {
            var eventCategory = groupKinds[i].eventCategory;
            var sampleKinds = groupKinds[i].sampleKinds;
            for (var j in sampleKinds) {
                var sampleKind = sampleKinds[j];
                for (var k in kindsToDraw) {
                    if (kindsToDraw[k].eventCategory == eventCategory && (kindsToDraw[k].sampleKinds.indexOf(sampleKind) != -1)) {
                        return true;
                    }
                }
            }
        }
    };

    /**
     * Given a list of sample kinds, create the associated treeview
     */
    this.populateSampleKindsTree = function(hostsSelected, hosts) {
        // First, find the individual sample kinds to draw
        var categoryAndSampleKindsForHosts = [];
        var sampleKinds = allKinds.sampleKinds;
        for (var i in sampleKinds) {
            if (!isSampleKindForHosts(sampleKinds[i], hosts)) {
                continue;
            }
            categoryAndSampleKindsForHosts.push(sampleKinds[i].categoryAndSampleKinds);
        }

        // But add the groups first
        var groups = allKinds.groups;
        for (var i in groups) {
            var groupName = groups[i].name;
            var categoriesAndSampleKinds = groups[i].kinds;
            if (shouldDrawGroup(categoriesAndSampleKinds, categoryAndSampleKindsForHosts) && !Set.contains(groupsDrawn, groupName)) {
                Set.add(groupsDrawn, groupName);
                addCustomGroup(groupName, categoriesAndSampleKinds);
            }
        }

        // Then add individual beans
        for (var i in categoryAndSampleKindsForHosts) {
            var eventCategory = categoryAndSampleKindsForHosts[i].eventCategory;
            var sampleKindsForCategory = categoryAndSampleKindsForHosts[i].sampleKinds;
            var selected = isSampleKindSelectedAmongBean(categoryAndSampleKindsForHosts[i]);
            if (!Set.contains(beansDrawn, eventCategory)) {
                Set.add(beansDrawn, eventCategory);
                addBean(rootNode, eventCategory, sampleKindsForCategory, selected);
            }
        }

        // Update the summary box for sample kinds
        updateSampleKindsSelectedSummary();
    };

    /**
     * Return the sample kinds portion of the query parameter for the dashboard
     */
    this.toURI = function() {
        var first = true;
        var uri = '';

        var kinds = Set.elements(selectedSampleKinds);
        for (var i in kinds) {
            var categoryAndSampleKind = kinds[i];
            if (first) {
                first = false;
            } else {
                uri += '&';
            }

            uri += 'category_and_sample_kind=' + categoryAndSampleKind;
        }

        return uri;
    };

    /**
     * Callback when a node is (de)selected
     */
     function nodeSelected(node) {
         var kinds = [];

         // If a group of hosts is selected
         if (node.hasChildren()) {
             for (var i in node.childList) {
                 var child = node.childList[i];
                 kinds.push(nodeSelected(child));
             }
         } else {
             kinds.push({eventCategory: node.parent.data.title, sampleKind: node.data.title});
         }

         return kinds;
     };

    /**
     * Final callback after a node is (de)selected
     */
    function nodeSelected(node) {
        var kinds = [];

        // If a group is selected
        if (node.hasChildren()) {
            for (var i in node.childList) {
                var child = node.childList[i];
                kinds.concat(nodeSelected(child));
            }
        } else {
            kinds.push({eventCategory: node.parent.data.title, sampleKind: node.data.title});
        }

        return kinds;
    };

    /**
     * Final callback after a node is (de)selected
     */
     function kindsSelected(selected, kinds) {
        // Update the internal list of selected kinds
        for (var i in kinds) {
            var kind = kinds[i];
            if (selected) {
                that.addSelectedSampleKind(kind);
            } else {
                removeSelectedSampleKind(kind);
            }
        }
    };

    /**
     * Select a pair eventCategory/sampleKind
     *
     * @visibleForTesting
     */
    this.addSelectedSampleKind = function(kind) {
        Set.add(selectedSampleKinds, sampleKindToString(kind));
        selectedSampleKindsUpdated();
    };

    /**
     * Deselect a pair eventCategory/sampleKind
     */
    function removeSelectedSampleKind(kind) {
        Set.remove(selectedSampleKinds, sampleKindToString(kind));
        selectedSampleKindsUpdated();
    };

    /**
     * Update internal states whenever the list of selected hosts is updated
     */
    function selectedSampleKindsUpdated() {
        // Remember selected nodes for the next page load
        Arecibo.InputForm.LocalStore.setLatestSampleKindsSelected(selectedSampleKinds);

        // Update the summary box
        updateSampleKindsSelectedSummary();
    }

    /**
     * Update the sample kinds list summary
     */
    function updateSampleKindsSelectedSummary() {
        $('#sample_kinds_summary_list').html('');

        var kinds = Set.elements(selectedSampleKinds);
        for (var i in kinds) {
            var kind = kinds[i];
            var kindItem = $('<li></li>').html(kind);
            $('#sample_kinds_summary_list').append(kindItem);
        }
    };

    function sampleKindToString(kind) {
        // We use ',' as a separator as it is used in the query for the dashboard
        return kind.eventCategory + ',' + kind.sampleKind;
    };

    /**
     * Create the top-level root node
     */
    function createRootNode() {
        treeSelector.dynatree({
            onSelect: function(flag, node) {
                // isLazy if it's a top-level node
                if (node.data.isLazy) {
                    // Load the children
                    onLazyReadCallback(node);
                    // Select all children
                    for (var i in node.childList) {
                        node.childList[i].select(flag);
                    }
                    node.expand(true);
                };

                var kinds = nodeSelected(node);
                kindsSelected(flag, kinds);
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
        if (node.data.areciboSampleKinds && !node.hasChildren()) {
            // Triggered when bean names are clicked on. This will populate
            // the sample kinds associated with this name
            addSampleKindsForBean(node, node.data.areciboSampleKinds);
        } else if (node.data.areciboKinds && !node.hasChildren()) {
            // Triggered when custom group names are clicked on. This will populate
            // the very next level below (bean names)
            addBeans(node, node.data.areciboKinds);
        }
    };

    /**
     * Add a node for a custom group
     */
    function addCustomGroup(groupName, kinds) {
        var opts = {
                title: groupName,
                isFolder: true,
                icon: false,
                hideCheckbox: true,
                expand: false,
                select: false
        };

        var child;
        if (isSampleKindSelectedAmongGroup(kinds)) {
            // At least one child is selected - don't make this subtree lazy
            child = rootNode.addChild(opts);
            addBeans(child, kinds);
        } else {
            // For performance reasons, we don't add the children yet but use a custom
            // attribute to cache them. This will be used in the onLazyRead() callback.
            opts['isLazy'] = true;
            opts['areciboKinds'] = kinds;
            child = rootNode.addChild(opts);
        }

        return child;
    };

    /**
     * Add the children bean names for a custom group or at the root level
     */
    function addBeans(node, groupBeans) {
        // Sort kinds alphabetically by category name (i.e. bean name)
        var groupBeans = sort(groupBeans, 'eventCategory');
        for (var i in groupBeans) {
            var groupMember = groupBeans[i];
            var eventCategory = groupMember['eventCategory'];
            var sampleKinds = groupMember['sampleKinds'];
            var selected = isSampleKindSelectedAmongBean(groupMember);
            addBean(node, eventCategory, sampleKinds, selected);
        }
    };

    /**
     * Add a single bean
     */
    function addBean(parentNode, bean, sampleKinds, selected) {
        var opts = {
                title: bean,
                isFolder: true,
                icon: false,
                hideCheckbox: true,
                expand: false,
                select: selected
        };

        // If at least one child node is selected, expand the father
        if (selected) {
            var child = parentNode.addChild(opts);
            addSampleKindsForBean(child, sampleKinds);
            // Note! This needs to happen after the child is added to the father
            parentNode.expand(true);
        } else {
            // For performance reasons, we don't add the children yet but use a custom
            // attribute to cache the title of the future children. This will be used
            // in the onSelect() callback.
            opts['isLazy'] = true;
            opts['areciboSampleKinds'] = sampleKinds;
            parentNode.addChild(opts);
            // If at least one child node is not selected, don't select the father
            // TODO Unfortunately, the following will deselect ALL children
            //parentNode.select(false);
        }
    };

    /**
     * Add the children sample kinds for a bean
     */
    function addSampleKindsForBean(node, sampleKinds) {
        // Sort sample kinds alphabetically
        sampleKinds = sampleKinds.sort();
        for (var i in sampleKinds) {
            var sampleKind = sampleKinds[i];
            var selected = isSampleKindSelected({eventCategory: node.data.title, sampleKind: sampleKind});
            addSampleKind(node, sampleKind, selected);
        }
    };

    /**
     * Add a node for a single sample kind
     */
    function addSampleKind(parentNode, sampleKind, selected) {
        parentNode.addChild({
            title: sampleKind,
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

    function isSampleKindSelectedAmongGroup(kinds) {
        for (var i in kinds) {
            var eventCategory = kinds[i].eventCategory;
            if (isSampleKindSelectedAmongBean(kinds[i])) {
                return true;
            }
        }

        return false;
    };

    function isSampleKindSelectedAmongBean(kinds) {
        var eventCategory = kinds.eventCategory;
        for (var j in kinds.sampleKinds) {
            var sampleKind = kinds.sampleKinds[j];
            if (isSampleKindSelected({eventCategory: eventCategory, sampleKind: sampleKind})) {
                return true;
            }
        }

        return false;
    };

    function isSampleKindSelected(kind) {
        return Set.contains(selectedSampleKinds, sampleKindToString(kind));
    };
};