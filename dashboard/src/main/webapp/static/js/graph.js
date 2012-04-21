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
        // The actual data
        timeseries: [],
        // Scale factors for all graphs (i.e. mapping between (host + kind) and scale factor)
        scales: {},
        // The actual graph object
        graph: null,
        // Settings for the graph, as defined by the left "Rendering" buttons
        graph_settings: {
            interpolation: 'linear',
            offset: 'zero',
            renderer: 'line'
        },
        // Permalink temporary data for the graphs.
        graph_permalink: [],
        // The graph palette, used to generate the colors for the different graphs
        graph_palette: new Rickshaw.Color.Palette({ scheme: 'colorwheel' }),
    };

    // Set the radio buttons properly
    $('input:radio[name="renderer"]').filter('[value="' + window.arecibo.graph_settings['renderer'] + '"]').attr('checked', true);
    $('input:radio[name="offset"]').filter('[value="' + window.arecibo.graph_settings['offset'] + '"]').attr('checked', true);
    $('input:radio[name="interpolation"]').filter('[value="' + window.arecibo.graph_settings['interpolation'] + '"]').attr('checked', true);

    updateGraph();
};

function updateGraph() {
    var url  = '/rest/1.0/host_samples' + window.location.search;

    // Create a debug link
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

        window.arecibo.timeseries.push(
            {
                color: window.arecibo.graph_palette.color(),
                data: data,
                name: sample['hostName'] + ' (' + sample['eventCategory'] + '::' + sample['sampleKind'] + ')'
            }
        );
    }

    // Make sure time series have the same number of data points
    fillSeries(window.arecibo.timeseries);

    // Draw the graph
    if (window.arecibo.timeseries.length > 0) {
        drawGraph();
    }
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

/*
 * D3.js magic
 */
function drawGraph() {
    $("#chart").children().remove();
    $("#legend").children().remove();
    $("#y_axis").children().remove();
    // Don't clear the slider!

    var graph = new Rickshaw.Graph({
        element: document.querySelector("#chart"),
        // width: ($('#chart_container').width() - $('#y_axis').width()) * 0.95,
        // height: 500,
        renderer: 'line',
        series: window.arecibo.timeseries
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
        element: document.querySelector('#legend')
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
        ticksTreatment: 'glow'
    });
    yAxis.render();

    var slider = new Rickshaw.Graph.RangeSlider({
        graph: graph,
        // Note: document.getElementById won't work here
        element: $('#slider')
    });

    var smoother = new Rickshaw.Graph.Smoother({
        graph: graph,
        element: document.getElementById('smoother')
    });

    var controls = new RenderControls({
        element: document.getElementById('side_panel'),
        graph: graph
    });

    updateGraphSettings(graph, window.arecibo.graph_settings);

    window.arecibo.graph = graph;
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