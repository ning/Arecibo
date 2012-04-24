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

function outerHTML(node) {
    // Gross hack - remove </input> tag for the command line runner as they're
    // not valid HTML anyways
    return $('<div>').append(node.clone()).html().replace(/\<\/input>/gi, '');
}

function verify(actual, expected) {
    expect(actual.length).toBe(1);
    expect(outerHTML(actual)).toBe(expected);
}

describe('The graph builder', function () {
    it('should be able to add a new graph container in an empty grid', function() {
        spyOn(window, 'buildGraphContainer').andReturn('<div id="GRAPH"></div>');
        $('html,body').append($('<div></div>').attr('id', 'graph_grid'));
        verify($('#graph_grid'), '<div id="graph_grid"></div>')

        addGraphContainer(1);
        verify($('#graph_grid'), '<div id="graph_grid"><div class="row show-grid row_graph_container"><div id="GRAPH"></div></div></div>');

        addGraphContainer(2);
        verify($('#graph_grid'), '<div id="graph_grid"><div class="row show-grid row_graph_container"><div id="GRAPH"></div><div id="GRAPH"></div></div></div>');

        addGraphContainer(3);
        verify($('#graph_grid'), '<div id="graph_grid"><div class="row show-grid row_graph_container"><div id="GRAPH"></div><div id="GRAPH"></div></div><div class="row show-grid row_graph_container"><div id="GRAPH"></div></div></div>');
    });

    it('should be able to create a new graph container', function() {
        spyOn(window, 'buildGraphRow').andReturn('<div id="A"></div>');
        spyOn(window, 'buildDisplayRow').andReturn('<div id="B"></div>');
        spyOn(window, 'buildControlsAndLegendRow').andReturn('<div id="C"></div>');
        spyOn(window, 'buildDebugRow').andReturn('<div id="D"></div>');

        var htmlBuilt = buildGraphContainer(1, 'Pweet');
        var htmlExpected = '<div class="span6"><div id="A"></div><div id="B"></div><div id="C"></div><div id="D"></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the display controls row', function() {
        var htmlBuilt = buildDisplayRow(1);
        var htmlExpected = '<div class="display_controls" id="display_controls_1"><a style="cursor: pointer; cursor: hand;"><i class="icon-plus-sign"></i></a><a style="cursor: pointer; cursor: hand;"><i class="icon-arrow-left"></i></a><a style="cursor: pointer; cursor: hand;"><i class="icon-arrow-right"></i></a><a style="cursor: pointer; cursor: hand;"><i class="icon-minus"></i></a><a style="cursor: pointer; cursor: hand;"><i class="icon-plus"></i></a></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the controls and legend row', function() {
        spyOn(window, 'buildGraphControlsColumn').andReturn('<div id="A"></div>');
        spyOn(window, 'buildLegendColumn').andReturn('<div id="B"></div>');

        var htmlBuilt = buildControlsAndLegendRow(1);
        var htmlExpected = '<div class="row controls_and_legend" id="controls_and_legend_1" style="display: none;"><div id="A"></div><div id="B"></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the graph row', function() {
        var htmlBuilt = buildGraphRow(1, 'Pweet');
        var htmlExpected = '<div class="row"><h5>Pweet</h5><div class="chart_container" id="chart_container_1"><div class="y_axis" id="y_axis_1"></div><div class="chart" id="chart_1"></div></div><div class="slider" id="slider_1"></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the form controls row', function() {
        spyOn(window, 'buildGraphControlsForm').andReturn('<form></form>');

        var htmlBuilt = buildGraphControlsColumn(1);
        var htmlExpected = '<div class="span3" id="graph_controls_1"><form></form><div><h6>Smoothing</h6><div id="smoother_container_1"></div></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the smoother container', function() {
        var htmlBuilt = buildSmootherContainer(1);
        var htmlExpected = '<div><h6>Smoothing</h6><div id="smoother_container_1"></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the legend row', function() {
        var htmlBuilt = buildLegendColumn(1);
        var htmlExpected = '<div class="span3"><div id="legend_container_1"><div id="legend_1"></div></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the debug row', function() {
        var htmlBuilt = buildDebugRow(1);
        var htmlExpected = '<div class="row"><div id="debug_container_1"></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the controls renderer fields', function() {
        spyOn(window, 'buildGraphControlsRendererFields').andReturn('<div id="A"></div>');
        spyOn(window, 'buildGraphControlsOffsetFields').andReturn('<div id="B"></div>');
        spyOn(window, 'buildGraphControlsInterpolationFields').andReturn('<div id="C"></div>');

        var htmlBuilt = buildGraphControlsForm(1);
        var htmlExpected = '<form class="form-inline" id="side_panel_1"><fieldset><h6>Rendering</h6><div id="A"></div><div id="B"></div><div id="C"></div></fieldset></form>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the controls renderer fields', function() {
        var htmlBuilt = buildGraphControlsRendererFields(1);
        var htmlExpected = '<div class="control-group toggler" id="renderer_form_1"><div class="controls"><label for="area_1"><input type="radio" value="area" name="renderer" id="area_1" checked="checked">area</label><label for="bar_1"><input type="radio" value="bar" name="renderer" id="bar_1">bar</label><label for="line_1"><input type="radio" value="line" name="renderer" id="line_1">line</label><label for="scatter_1"><input type="radio" value="scatterplot" name="renderer" id="scatter_1">scatter</label></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the controls offset fields', function() {
        var htmlBuilt = buildGraphControlsOffsetFields(1);
        var htmlExpected = '<div class="control-group" id="offset_form_1"><div class="controls"><label for="stack_1"><input type="radio" value="zero" name="offset" id="stack_1">stack</label><label for="stream_1"><input type="radio" value="wiggle" name="offset" id="stream_1">stream</label><label for="pct_1"><input type="radio" value="expand" name="offset" id="pct_1">pct</label><label for="value_1"><input type="radio" value="value" name="offset" id="value_1" checked="checked">value</label></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create the controls interpolation fields', function() {
        var htmlBuilt = buildGraphControlsInterpolationFields(1);
        var htmlExpected = '<div class="control-group" id="interpolation_form_1"><div class="controls"><label for="cardinal_1"><input type="radio" value="cardinal" name="interpolation" id="cardinal_1">cardinal</label><label for="linear_1"><input type="radio" value="linear" name="interpolation" id="linear_1" checked="checked">linear</label><label for="step_1"><input type="radio" value="step-after" name="interpolation" id="step_1">step</label></div></div>';
        verify(htmlBuilt, htmlExpected);
    });

    it('should be able to create all controls input fields', function() {
        var htmlBuilt = buildGraphControlsInputField(1, 'renderer', 'area', 'area', true, 'area');
        var htmlExpected = '<label for="area_1"><input type="radio" value="area" name="renderer" id="area_1" checked="checked">area</label>';
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'renderer', 'bar', 'bar', false, 'bar');
        htmlExpected = '<label for="bar_1"><input type="radio" value="bar" name="renderer" id="bar_1">bar</label>';
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'renderer', 'line', 'line', false, 'line');
        htmlExpected = '<label for="line_1"><input type="radio" value="line" name="renderer" id="line_1">line</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'renderer', 'scatter', 'scatterplot', false, 'scatter');
        htmlExpected = '<label for="scatter_1"><input type="radio" value="scatterplot" name="renderer" id="scatter_1">scatter</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'offset', 'stack', 'zero', false, 'stack');
        htmlExpected = '<label for="stack_1"><input type="radio" value="zero" name="offset" id="stack_1">stack</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'offset', 'stream', 'wiggle', false, 'stream');
        htmlExpected = '<label for="stream_1"><input type="radio" value="wiggle" name="offset" id="stream_1">stream</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'offset', 'pct', 'expand', false, 'pct');
        htmlExpected = '<label for="pct_1"><input type="radio" value="expand" name="offset" id="pct_1">pct</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'offset', 'value', 'value', true, 'value');
        htmlExpected = '<label for="value_1"><input type="radio" value="value" name="offset" id="value_1" checked="checked">value</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'interpolation', 'cardinal', 'cardinal', false, 'cardinal');
        htmlExpected = '<label for="cardinal_1"><input type="radio" value="cardinal" name="interpolation" id="cardinal_1">cardinal</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'interpolation', 'linear', 'linear', true, 'linear');
        htmlExpected = '<label for="linear_1"><input type="radio" value="linear" name="interpolation" id="linear_1" checked="checked">linear</label>'
        verify(htmlBuilt, htmlExpected);

        htmlBuilt = buildGraphControlsInputField(1, 'interpolation', 'step', 'step-after', false, 'step');
        htmlExpected = '<label for="step_1"><input type="radio" value="step-after" name="interpolation" id="step_1">step</label>'
        verify(htmlBuilt, htmlExpected);
    });
});