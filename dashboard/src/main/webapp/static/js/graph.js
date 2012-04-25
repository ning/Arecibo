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

function renderGraph() {
    // UI setup (Ajax handlers, etc.)
    initializeUI();

    // Mapping between graphIds and graphs meta-objects
    window.arecibo.graphs = {};
    // Mapping between sampleCategory, sampleKind and graphIds
    window.arecibo.graph_per_kind = {};
    // Metadata for colors
    window.arecibo.colors = {
        // Global mapping between hosts and colors to be able to keep the same color
        // across graphs on a per host basis
        host_colors: {},
        // The graph palette, used to generate the colors for the different graphs
        palette: new Rickshaw.Color.Palette({ scheme: 'colorwheel' })
    };
    // Default settings for the graphs
    window.arecibo.graph_settings = {
        // linear interpolation by default, i.e. straight lines between points
        interpolation: 'linear',
        // graphs offset should be the value of the data points by default
        offset: 'value',
        renderer: 'line'
    };

    // Set the radio buttons properly
    $('input:radio[name="renderer"]').filter('[value="' + window.arecibo.graph_settings['renderer'] + '"]').attr('checked', true);
    $('input:radio[name="offset"]').filter('[value="' + window.arecibo.graph_settings['offset'] + '"]').attr('checked', true);
    $('input:radio[name="interpolation"]').filter('[value="' + window.arecibo.graph_settings['interpolation'] + '"]').attr('checked', true);

    createGraphs();
};

function createGraphs() {
    var url  = '/rest/1.0/host_samples' + window.location.search;

    // Create a debug link
    // TODO
    $('<a>',{
        text: 'See raw data',
        title: 'Raw data',
        href: url + '&pretty=true'
    }).appendTo('#debug_container');

    callArecibo(url, "createGraph");
}

// Initially called by the first callback to create the
// graph objects and the grid (containers)
function createGraph(payload) {
    var graphIds = populateSamples(payload, false);

    for (var i in graphIds) {
        var graphId = graphIds[i];
        addGraphContainer(window.arecibo.graphs[graphId], graphIds.length);
        drawGraph(window.arecibo.graphs[graphId]);
    }
}

// Succeeding invocations
function refreshGraph(payload) {
    var graphIds = populateSamples(payload, true);

    for (var i in graphIds) {
        var graphId = graphIds[i];
        var graph = window.arecibo.graphs[graphId].graph;

        // Rickshaw doesn't redraw the legend for us, we need to do it
        $('#legend_' + graphId).empty();
        drawLegend(graph, graphId);

        // Render the new graph
        graph.update();
    }
}

function populateSamples(payload, refresh) {
    // payload is an array of objects:
    // {
    //    eventCategory: "JVMMemory"
    //    hostName: "box.company.com"
    //    sampleKind: "heapMax"
    //    samples: "1333137065,0.0,1333137095,0.0"
    // }
    var updatedGraphIds = Set.makeSet();
    var refreshed = Set.makeSet();

    // Prepare the time series
    for (var j in payload) {
        var sample = payload[j];
        var hostName = sample['hostName'];
        var sampleCategory = sample['eventCategory'];
        var sampleKind = sample['sampleKind'];
        var csv = sample['samples'].split(',');

        // Register the new graph if needed
        var graph = getOrCreateGraphMetaObject(sampleCategory, sampleKind);
        Set.add(updatedGraphIds, graph.graphId);
        Set.add(graph.hosts, hostName);

        // Final data in the form [{x:, y:}, {}, ...] for Richshaw
        var data = [];
        for (var i = 0; i < csv.length; i++) {
            tmp = {x:0, y:0};
            tmp.x = parseInt(csv[i]);
            tmp.y = parseFloat(csv[i+1]);
            if (!isNaN(tmp.x) && !isNaN(tmp.y) && !(tmp.x == Infinity) && !(tmp.y == Infinity)) {
                data.push(tmp);
                if (!graph.startDate || tmp.x < graph.startDate.getTime() / 1000) {
                    graph.startDate = new Date(tmp.x * 1000);
                }
                if (!graph.endDate || tmp.x > graph.endDate.getTime() / 1000) {
                    graph.endDate = new Date(tmp.x * 1000);
                }
            }
            i++;
        }

        // Determine the color to use for this host
        if (!window.arecibo.colors.host_colors[hostName]) {
            window.arecibo.colors.host_colors[hostName] = window.arecibo.colors.palette.color();
        }

        if (refresh && !Set.contains(refreshed, graph.graphId)) {
            graph.timeserie.splice(0, graph.timeserie.length);
            Set.add(refreshed, graph.graphId);
        }
        // Push to data for Rickshaw
        graph.timeserie.push(
            {
                color: window.arecibo.colors.host_colors[hostName],
                data: data,
                name: hostName
            }
        );
    }

    // Make sure time series have the same number of data points
    for (var graphId in window.arecibo.graphs) {
        fillSeries(window.arecibo.graphs[graphId].timeserie);
    }

    return Set.elements(updatedGraphIds);
}

// Function that manages the global registry for graph. It is also responsible
// for issuing graphIds
function getOrCreateGraphMetaObject(sampleCategory, sampleKind) {
    // Potential new graphId
    var graphId = Set.size(window.arecibo.graphs) + 1;

    if (!window.arecibo.graph_per_kind[sampleCategory]) {
        window.arecibo.graph_per_kind[sampleCategory] = {};
    }

    if (!window.arecibo.graph_per_kind[sampleCategory][sampleKind]) {
        // New graph
        window.arecibo.graph_per_kind[sampleCategory][sampleKind] = graphId;
    } else {
        graphId = window.arecibo.graph_per_kind[sampleCategory][sampleKind];
    }

    // Does the graph for this sampleCategory and sampleKind already exists?
    var graph = getGraphMetaObjectById(graphId);
    if (graph) {
        return graph;
    }

    // Nope, create the graph meta-object
    graph = {
        timeserie: [],
        graph: null,
        hosts: Set.makeSet(),
        sampleCategory: sampleCategory,
        sampleKind: sampleKind,
        graphId: graphId,
        startDate: null,
        endDate: null,
        periodicXhrTimeout: null
    };
    setGraphMetaObjectById(graphId, graph);

    return graph;
}

// Given a graphId, retrieve the associated graph meta object
function getGraphMetaObjectById(graphId) {
    return window.arecibo.graphs[graphId];
}

// Given a graphId, set the associated graph meta object
function setGraphMetaObjectById(graphId, graph) {
    window.arecibo.graphs[graphId] = graph;
}

/*
 * Normalize the number of data points per time series by repeating
 * the last value.
 */
function fillSeries(series) {
    var data = series.map(function(s) { return s.data });
    var maxSeriesLength = Math.max.apply(null, data.map(function(d) { return d.length }));

    data.forEach(function(d) {
        var maxX = d[d.length - 1].x;
        var y = d[d.length - 1].y;
        while (d.length < maxSeriesLength) {
            d.push({ x: maxX + d.length - maxSeriesLength, y: y });
        }
    });
}

function drawGraph(item) {
    var graphId = item.graphId;

    var graph = new Rickshaw.Graph({
        element: document.querySelector("#chart_" + graphId),
        width: 0.90 * $('#chart_container_' + graphId).parent().width(),
        series: item.timeserie
    });
    item.graph = graph;
    graph.render();

    // Interactive Hover Details

    // XXX We're hitting https://github.com/shutterstock/rickshaw/issues/37
    // and https://github.com/shutterstock/rickshaw/issues/39 which seem
    // to cause spinloops in some situations
    //
    // Show the series value and formatted date and time on hover
    // var hoverDetail = new Rickshaw.Graph.HoverDetail({
    //     graph: graph
    // });

    // Interactive Legend
    drawLegend(graph, graphId);

    // Axes and Tick Marks

    // Add a basic x-axis with time values
    var time = new Rickshaw.Fixtures.Time();
    var seconds = time.unit('seconds');
    var axes = new Rickshaw.Graph.Axis.Time({
        graph: graph,
        timeUnit: seconds
    });
    axes.render();

    // Add a basic y-axis
    var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT, // TODO
        ticksTreatment: 'glow',
        orientation: 'left',
        element: document.getElementById('y_axis_' + graphId)
    });
    yAxis.render();

    // var slider = new Rickshaw.Graph.RangeSlider({
    //     graph: graph,
    //     // Note: document.getElementById won't work here
    //     element: $('#slider_' + graphId)
    // });

    var smoother = new Rickshaw.Graph.Smoother({
        graph: graph,
        element: document.getElementById('smoother_container_' + graphId)
    });

    var controls = new RenderControls({
        graph: graph,
        element: document.getElementById('side_panel_' + graphId)
    });

    updateGraphSettings(graph, window.arecibo.graph_settings);
}

// Populate and configure the legend element
function drawLegend(graph, graphId) {
    // Add a basic legend
    var legend = new Rickshaw.Graph.Legend({
        graph: graph,
        element: document.querySelector('#legend_' + graphId)
    });

    // Add functionality to toggle series' visibility on and off
    var shelving = new Rickshaw.Graph.Behavior.Series.Toggle({
        graph: graph,
        legend: legend
    });

    // Add drag-and-drop functionality to re-order the stack
    var order = new Rickshaw.Graph.Behavior.Series.Order({
        graph: graph,
        legend: legend
    });

    // Highlight each series on hover within the legend
    var highlighter = new Rickshaw.Graph.Behavior.Series.Highlight({
        graph: graph,
        legend: legend
    });
}

// The following is inspired from http://shutterstock.github.com/rickshaw/examples/js/extensions.js

function updateGraphSettings(graph, settings) {
    graph.setRenderer(settings.renderer);
    graph.interpolation = settings.interpolation;

    if (settings.offset == 'value') {
        graph.renderer.unstack = true;
        graph.offset = 'zero';
    } else {
        graph.renderer.unstack = false;
        graph.offset = settings.offset;
    }

    // Force a stroke-width (line width) of 1 pixel
    graph.renderer.strokeWidth = 1;

    graph.render();
}

var RenderControls = function(args) {
    this.initialize = function() {
        this.element = args.element;
        this.graph = args.graph;
        this.settings = this.serialize();

        this.inputs = {
            renderer: this.element.elements.renderer,
            interpolation: this.element.elements.interpolation,
            offset: this.element.elements.offset
        };

        this.element.addEventListener('change', function(e) {
            this.settings = this.serialize();

            if (e.target.name == 'renderer') {
                this.setDefaultOffset(e.target.value);
            }

            this.syncOptions();
            this.settings = this.serialize();

            // Update the global settings for the permalink
            window.arecibo.graph_settings = this.settings;

            updateGraphSettings(this.graph, this.settings);
        }.bind(this), false);
    }

    this.serialize = function() {
        var values = {};
        var pairs = $(this.element).serializeArray();

        pairs.forEach(function(pair) {
            values[pair.name] = pair.value;
        });

        return values;
    };

    this.syncOptions = function() {
        var options = this.rendererOptions[this.settings.renderer];

        Array.prototype.forEach.call(this.inputs.interpolation, function(input) {
            if (options.interpolation) {
                input.disabled = false;
                input.parentNode.classList.remove('disabled');
            } else {
                input.disabled = true;
                input.parentNode.classList.add('disabled');
            }
        });

        Array.prototype.forEach.call(this.inputs.offset, function(input) {
            if (options.offset.filter(function(o) { return o == input.value }).length) {
                input.disabled = false;
                input.parentNode.classList.remove('disabled');

            } else {
                input.disabled = true;
                input.parentNode.classList.add('disabled');
            }
        }.bind(this));

    };

    this.setDefaultOffset = function(renderer) {
        var options = this.rendererOptions[renderer]; 

        if (options.defaults && options.defaults.offset) {
            Array.prototype.forEach.call(this.inputs.offset, function(input) {
                if (input.value == options.defaults.offset) {
                    input.checked = true;
                } else {
                    input.checked = false;
                }
            }.bind(this));
        }
    };

    this.rendererOptions = {
        area: {
            interpolation: true,
            offset: ['zero', 'wiggle', 'expand', 'value'],
            defaults: { offset: 'zero' }
        },
        line: {
            interpolation: true,
            offset: ['expand', 'value'],
            defaults: { offset: 'value' }
        },
        bar: {
            interpolation: false,
            offset: ['zero', 'wiggle', 'expand', 'value'],
            defaults: { offset: 'zero' }
        },
        scatterplot: {
            interpolation: false,
            offset: ['value'],
            defaults: { offset: 'value' }
        }
    };

    this.initialize();
};

// Add a new graph container in the grid
function addGraphContainer(graph, nbGraphs) {
    var graphId = graph.graphId;
    var graphTitle = graph.sampleCategory + ': ' + graph.sampleKind;
    var alone = false;

    // Do we need an extra row?
    if (graphId % 2 == 1) {
        $("#graph_grid").append($('<div></div>').attr('class', 'row show-grid row_graph_container'));
        // Is the graph alone on the last row?
        if (graphId == nbGraphs) {
            alone = true;
        }
    }

    // Create the container
    var graphContainer = buildGraphContainer(graphId, graphTitle, alone);

    // Find the latest row and add the new container
    $("#graph_grid div.row_graph_container:last").append(graphContainer);
}

// Build the necessary elements for a new graph
function buildGraphContainer(graphId, graphTitle, alone) {
    var span = 'span6';
    if (alone) {
        span = 'span12';
    }

    var graphRow = buildGraphRow(graphId, graphTitle);
    var controlsAndLegendRow = buildControlsAndLegendRow(graphId);
    var displayRow = buildDisplayRow(graphId);
    var debugRow = buildDebugRow(graphId);

    return $('<div></div>')
                .attr('class', span)
                .append(graphRow)
                .append(displayRow)
                .append(controlsAndLegendRow)
                .append(debugRow);
}

// Build the actual graph container
function buildGraphRow(graphId, graphTitle) {
    return $('<div></div>')
                .attr('class', 'row')
                .append($('<h5></h5>').text(graphTitle))
                .append($('<div></div>')
                            .attr('class', 'chart_container')
                            .attr('id', 'chart_container_' + graphId)
                            .append($('<div></div>').attr('class', 'y_axis').attr('id', 'y_axis_' + graphId))
                            .append($('<div></div>').attr('class', 'chart').attr('id', 'chart_' + graphId))
                        )
                .append($('<div></div>').attr('class', 'slider').attr('id', 'slider_' + graphId));
}

function buildControlsAndLegendRow(graphId) {
    var graphControlsRow = buildGraphControlsColumn(graphId);
    var legendRow = buildLegendColumn(graphId);

    return $('<div></div>')
                .attr('class', 'row controls_and_legend')
                .attr('id', 'controls_and_legend_' + graphId)
                .attr('style', 'display: none;')
                .append(graphControlsRow)
                .append(legendRow);
}

// Build the form controls container
function buildGraphControlsColumn(graphId) {
    return $('<div></div>')
                .attr('class', 'span3')
                .attr('id', 'graph_controls_' + graphId)
                .append(buildGraphControlsForm(graphId))
                .append(buildSmootherContainer(graphId));
}

// Build the container where the legend will be injected
function buildLegendColumn(graphId) {
    return $('<div></div>')
                .attr('class', 'span3')
                .append($('<div></div>')
                            .attr('id', 'legend_container_' + graphId)
                            .append($('<div></div>').attr('id', 'legend_' + graphId))
                        );
}

// Build the smoother container
function buildSmootherContainer(graphId) {
    return $('<div></div>')
                .append($('<h6></h6>').text('Smoothing'))
                .append($('<div></div>').attr('id', 'smoother_container_' + graphId));
}

// Build the row with the display controls (zoom, shift, etc.)
function buildDisplayRow(graphId) {
    // Show/Hide the graph controls
    var toggleBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-plus-sign')
                            .click(function() {
                                // See buildControlsAndLegendRow()
                                $('#controls_and_legend_' + graphId).toggle();
                            })
                        );

    // Go back in time
    var shiftLeftBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-arrow-left')
                            .click(function() { shiftLeft(graphId); return false; })
                        );
    // Go forward in time
    var shiftRightBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-arrow-right')
                            .click(function() { shiftRight(graphId); return false; })
                        );

    // Zoom in in time
    var zoomInBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-plus')
                            .click(function() { zoomIn(graphId); return false; })
                        );
    // Zoom out in time
    var zoomOutBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-minus')
                            .click(function() { zoomOut(graphId); return false; })
                        );

    // Real-time mode
    var realtimeBtn = $('<a></a>')
                        .attr('style', 'cursor: pointer; cursor: hand;')
                        .append(
                            $('<i></i>')
                            .attr('class', 'icon-time')
                            .click(function() { realtime(graphId); return false; })
                        );


    return $('<div></div>')
                .attr('class', 'display_controls')
                .attr('id', 'display_controls_' + graphId)
                .append(toggleBtn)
                .append(shiftLeftBtn)
                .append(shiftRightBtn)
                .append(zoomOutBtn)
                .append(zoomInBtn)
                .append(realtimeBtn);
}

// Build the debug row for a graph: this is where we store extra metadata
// information, such as the direct link to the raw data
function buildDebugRow(graphId) {
    return $('<div></div>')
                .attr('class', 'row')
                .append($('<div></div>').attr('id', 'debug_container_' + graphId));
}

// Build the complete form for the controls
function buildGraphControlsForm(graphId) {
    var rendererFields = buildGraphControlsRendererFields(graphId);
    var offsetFields = buildGraphControlsOffsetFields(graphId);
    var interpolationFields = buildGraphControlsInterpolationFields(graphId);

    var fieldSet = $('<fieldset></fieldset>')
                        .append($('<h6></h6>').text('Rendering'))
                        .append(rendererFields)
                        .append(offsetFields)
                        .append(interpolationFields);

    return $('<form></form>')
                .attr('class', 'form-inline')
                .attr('id', 'side_panel_' + graphId)
                .append(fieldSet);
}

// Build the controls for the renderers (how to draw the data points)
function buildGraphControlsRendererFields(graphId) {
    var areaRendererField = buildGraphControlsInputField(1, 'renderer', 'area', 'area', true, 'area');
    var barRendererField = buildGraphControlsInputField(1, 'renderer', 'bar', 'bar', false, 'bar');
    var lineRendererField = buildGraphControlsInputField(1, 'renderer', 'line', 'line', false, 'line');
    var scatterRendererField = buildGraphControlsInputField(1, 'renderer', 'scatter', 'scatterplot', false, 'scatter');

    return $('<div></div')
                .attr('class', 'control-group toggler')
                .attr('id', 'renderer_form_' + graphId)
                .append(
                    $('<div></div>')
                    .attr('class', 'controls')
                    .append(areaRendererField)
                    .append(barRendererField)
                    .append(lineRendererField)
                    .append(scatterRendererField)
                );
}

// Build the controls for the offsets (how to position the different graph relatively to each other)
function buildGraphControlsOffsetFields(graphId) {
    var stackOffsetField = buildGraphControlsInputField(1, 'offset', 'stack', 'zero', false, 'stack');
    var streamOffsetField = buildGraphControlsInputField(1, 'offset', 'stream', 'wiggle', false, 'stream');
    var pctOffsetField = buildGraphControlsInputField(1, 'offset', 'pct', 'expand', false, 'pct');
    var valueOffsetField = buildGraphControlsInputField(1, 'offset', 'value', 'value', true, 'value');

    return $('<div></div')
                .attr('class', 'control-group')
                .attr('id', 'offset_form_' + graphId)
                .append(
                    $('<div></div>')
                    .attr('class', 'controls')
                    .append(stackOffsetField)
                    .append(streamOffsetField)
                    .append(pctOffsetField)
                    .append(valueOffsetField)
                );
}

// Build the controls for the interpolation (how to join data points within a graph)
function buildGraphControlsInterpolationFields(graphId) {
    var cardinalInterpolationField = buildGraphControlsInputField(1, 'interpolation', 'cardinal', 'cardinal', false, 'cardinal');
    var linearInterpolationField = buildGraphControlsInputField(1, 'interpolation', 'linear', 'linear', true, 'linear');
    var stepInterpolationField = buildGraphControlsInputField(1, 'interpolation', 'step', 'step-after', false, 'step');

    return $('<div></div')
                .attr('class', 'control-group')
                .attr('id', 'interpolation_form_' + graphId)
                .append(
                    $('<div></div>')
                    .attr('class', 'controls')
                    .append(cardinalInterpolationField)
                    .append(linearInterpolationField)
                    .append(stepInterpolationField)
                );
}

// Build one label/input combo for the graph controls
function buildGraphControlsInputField(graphId, name, id, value, checked, text) {
    var inputId = id + '_' + graphId;
    var inputElement =
        $('<input>')
            .attr('type', 'radio')
            .attr('value', value)
            .attr('name', name)
            .attr('id', inputId)
            .attr('checked', checked);

    return $('<label></label>')
            .attr('for', inputId)
            .append(inputElement)
            .append(text);
}