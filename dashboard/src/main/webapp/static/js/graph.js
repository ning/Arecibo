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

    // Global variables
    window.arecibo = {
        // Dashboard location, e.g. 'http://127.0.0.1:8080'
        uri: window.location.origin,
        // Mapping between sample kinds and (timeserie, graph) tuples
        graphs: {},
        // Default settings for the graphs
        graph_settings: {
            // linear interpolation by default, i.e. straight lines between points
            interpolation: 'linear',
            // graphs offset should be the value of the data points by default
            offset: 'value',
            renderer: 'line'
        },
        // The graph palette, used to generate the colors for the different graphs
        graph_palette: new Rickshaw.Color.Palette({ scheme: 'colorwheel' }),
    };

    // Set the radio buttons properly
    $('input:radio[name="renderer"]').filter('[value="' + window.arecibo.graph_settings['renderer'] + '"]').attr('checked', true);
    $('input:radio[name="offset"]').filter('[value="' + window.arecibo.graph_settings['offset'] + '"]').attr('checked', true);
    $('input:radio[name="interpolation"]').filter('[value="' + window.arecibo.graph_settings['interpolation'] + '"]').attr('checked', true);

    $('#controls_btn').click(function() {
        $('#graph_controls').toggle();
    });

    updateGraph();
};

function updateGraph() {
    var url  = '/rest/1.0/host_samples' + window.location.search;

    // Create a debug link
    // TODO
    $('<a>',{
        text: 'See raw data',
        title: 'Raw data',
        href: url + '&pretty=true'
    }).appendTo('#debug_container');

    callArecibo(url, "populateSamples");
}

/*
 * Add samples to a graph (callback from the dashboard)
 *
 * @param {String}  payload     Data (for a single host) sent from the collector
 */
function populateSamples(payload) {
    // payload is an array of objects:
    // {
    //    eventCategory: "JVMMemory"
    //    hostName: "box.company.com"
    //    sampleKind: "heapMax"
    //    samples: "1333137065,0.0,1333137095,0.0"
    // }

    // Prepare the time series
    for (var j in payload) {
        var sample = payload[j];

        // New sample kind?
        var sampleKind = sample['sampleKind'];
        if (!window.arecibo.graphs[sampleKind]) {
            window.arecibo.graphs[sampleKind] = {
                timeserie: [],
                graph: null,
            }
        }

        var csv = sample['samples'].split(",");
        // Build data in the form [{x:, y:}, {}, ...]
        var data = [];
        var lastX = -1;
        for (var i = 0; i < csv.length; i++) {
            tmp = {x:0, y:0};
            tmp.x = parseInt(csv[i]);
            tmp.y = parseFloat(csv[i+1]);
            if (!isNaN(tmp.x) && !isNaN(tmp.y) && (tmp.x > lastX) && !(tmp.x == Infinity) && !(tmp.y == Infinity)) {
                data.push(tmp);
                lastX = tmp.x;
            }
            i++;
        }

        window.arecibo.graphs[sampleKind].timeserie.push(
            {
                color: window.arecibo.graph_palette.color(),
                data: data,
                name: sample['hostName']
            }
        );
    }

    // Make sure time series have the same number of data points
    for (var sampleKind in window.arecibo.graphs) {
        fillSeries(window.arecibo.graphs[sampleKind].timeserie);
    }

    drawAllGraphs();
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

// window.arecibo.graphs has been populated by the Ajax callback, now
// populate the grid with the graphs
function drawAllGraphs() {
    var nbGraphs = Set.size(window.arecibo.graphs);
    var i = 1;
    for (var sampleKind in window.arecibo.graphs) {
        addGraphContainer(i, sampleKind, nbGraphs);
        drawGraph(i, sampleKind);
        i++;
    }
}

function drawGraph(graphId, sampleKind) {
    var graph = new Rickshaw.Graph({
        element: document.querySelector("#chart_" + graphId),
        width: 0.90 * $('#chart_container_' + graphId).parent().width(),
        series: window.arecibo.graphs[sampleKind].timeserie
    });
    graph.render();

    // Interactive Hover Details

    // Show the series value and formatted date and time on hover
    var hoverDetail = new Rickshaw.Graph.HoverDetail({
        graph: graph
    });

    // Interactive Legend

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

    var slider = new Rickshaw.Graph.RangeSlider({
        graph: graph,
        // Note: document.getElementById won't work here
        element: $('#slider_' + graphId)
    });

    var smoother = new Rickshaw.Graph.Smoother({
        graph: graph,
        element: document.getElementById('smoother_container_' + graphId)
    });

    var controls = new RenderControls({
        element: document.getElementById('side_panel_' + graphId),
        graph: graph
    });

    updateGraphSettings(graph, window.arecibo.graph_settings);

    window.arecibo.graphs[sampleKind].graph = graph;
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
function addGraphContainer(graphId, sampleKind, nbGraphs) {
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
    var graphContainer = buildGraphContainer(graphId, sampleKind, alone);

    // Find the latest row and add the new container
    $("#graph_grid div.row_graph_container:last").append(graphContainer);
}

// Build the necessary elements for a new graph
function buildGraphContainer(graphId, sampleKind, alone) {
    var span = 'span6';
    if (alone) {
        span = 'span12';
    }

    var graphRow = buildGraphRow(graphId);
    var controlsAndLegendRow = buildControlsAndLegendRow(graphId);
    var smootherRow = buildSmootherRow(graphId);
    var debugRow = buildDebugRow(graphId);

    return $('<div></div>')
                .attr('class', span)
                .append($('<h5></h5>').text(sampleKind))
                .append(graphRow)
                .append(controlsAndLegendRow)
                .append(smootherRow)
                .append(debugRow);
}

// Build the actual graph container
function buildGraphRow(graphId) {
    return $('<div></div>')
                .attr('class', 'row')
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
                .attr('class', 'row')
                .append(graphControlsRow)
                .append(legendRow);
}

// Build the form controls container
function buildGraphControlsColumn(graphId) {
    return $('<div></div>')
                .attr('class', 'span3')
                .attr('id', 'graph_controls_' + graphId)
                .append(buildGraphControlsForm(graphId));
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
function buildSmootherRow(graphId) {
    return $('<div></div>')
                .attr('class', 'row')
                .append($('<h6></h6>').text('Smoothing'))
                .append($('<div></div>').attr('id', 'smoother_container_' + graphId));
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