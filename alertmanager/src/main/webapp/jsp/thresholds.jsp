<%@ page import="com.ning.arecibo.alert.confdata.AlertingConfig" %>
<%@ page import="com.ning.arecibo.alert.confdata.ThresholdContextAttr" %>
<%@ page import="com.ning.arecibo.alert.confdata.ThresholdDefinition" %>
<%@ page import="com.ning.arecibo.alert.confdata.ThresholdQualifyingAttr" %>
<%@include file="../global_includes/header.jsp" %>

<%@include file="../global_includes/navbar.jsp" %>

<%--
  ~ Copyright 2010-2012 Ning, Inc.
  ~
  ~ Ning licenses this file to you under the Apache License, version 2.0
  ~ (the "License"); you may not use this file except in compliance with the
  ~ License.  You may obtain a copy of the License at:
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  ~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  ~ License for the specific language governing permissions and limitations
  ~ under the License.
  --%>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_thresholds").attr("class", "active");

        $("#thresholds_form").collapse();
        $("#thresholds_form").toggle();
        $("#add_form").click(function ()
        {
            $("#thresholds_form").toggle();
        });

        $('#table_thresholds').dataTable({
            "sDom": "<'row'<'span6'l><'span6'f>r>t<'row'<'span6'i><'span6'p>>",
            "sPaginationType": "bootstrap",
            "oLanguage": {
                "sLengthMenu": "_MENU_ records per page"
            }
        });
    });
</script>

<div class="container">
  <div class="page-header">
    <h1>Threshold Definitions <i class="icon-plus" id="add_form" style="cursor: pointer;"></i></h1>
  </div>
</div>

<jsp:useBean id="it"
             type="com.ning.arecibo.alertmanager.models.ThresholdDefinitionsModel"
             scope="request">
</jsp:useBean>

<div class="container" id="thresholds_form">
    <form class="form-horizontal" action="/ui/thresholds" method="post" name="thresholds_form">
    <fieldset>
      <legend>Add a Threshold Definition</legend>

      <div class="control-group">
        <p>A Threshold Definition is a specific rule for defining conditions that should trigger an alert. It is specified by choosing an Event Type and an associated Attribute Type. These must correspond to valid, monitored events and attributes within the Arecibo system, in order to be triggered. These types are case sensitive, so be sure to spell them correctly, observing case. You can refer to the Arecibo Dashboard to view currently active monitored events, for spelling. The specified attribute type must be a numeric valued field.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="threshold_name">Threshold name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="threshold_name" id="threshold_name">
          <p class="help-block">Short identifier for this threshold. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="event_name">Event name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="event_name" id="event_name">
          <p class="help-block">Usually JMX Bean. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="attribute_name">Attribute name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="attribute_name" id="attribute_name">
          <p class="help-block">Usually JMX attribute. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="alerting_configuration">Alerting configuration</label>
        <div class="controls">
            <select multiple="multiple" name="alerting_configuration" id="alerting_configuration">
                <% if (it != null) {
                    for (final AlertingConfig alertingConfiguration : it.getAllAlertingConfigurations()) { %>
                    <option value="<%= alertingConfiguration.getId() %>"><%= alertingConfiguration.getAlertingConfigurationName() %></option>
                <% } } %>
            </select>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="min_threshold">Minimum threshold</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="min_threshold" id="min_threshold">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="max_threshold">Maximum threshold</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="max_threshold" id="max_threshold">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="min_samples">Minimum samples</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="min_samples" id="min_samples">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="max_samples">Maximum samples window (ms)</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="max_samples" id="max_samples">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="clearing_interval">Clearing interval (ms)</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="clearing_interval" id="clearing_interval">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="qualifying_attribute_type">Qualifying Attribute Type</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="qualifying_attribute_type" id="qualifying_attribute_type">
        </div>
      </div>
      <div class="control-group">
        <label class="control-label" for="qualifying_attribute_value">Qualifying Attribute Value</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="qualifying_attribute_value" id="qualifying_attribute_value">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="context_attribute_type">Context Attribute Type</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="context_attribute_type" id="context_attribute_type">
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save changes</button>
      </div>
    </fieldset>
  </form>

  <div>
    <p>A Threshold Definition includes a reference an Alerting Configuration, which allows sharing configuration between multiple threshold definitions. Each Threshold Definition requires an existing Alerting Configuration, so it may be necessary to set up an Alerting Configuration before creating a Threshold Definition.</p>

    <p>There are 2 optional types of attributes that affect how events are selected for alert monitoring, and how they are managed and tracked internally.</p>

    <p>Qualifying Attributes are like event selectors, and are comprised of attribute name -> value pairs. These attributes must be part of the same event that the associated threshold is defined for. Any number of Qualifying Attributes can be specified for a Threshold Definition (including zero). The most common examples of Qualifying Attributes within Arecibo are for selecting by core type (e.g. 'deployedType->collector'), and/or for selecting by config sub path (e.g. 'deployedConfigSubPath->cluster1'). Any attribute within an Arecibo monitored event can be used as a Qualifying Attribute.</p>

    <p>Context Attributes provide bookkeeping for alerts once they have been triggered. They indicate a particular attributeType, whose value is used to independently track alerts, which are otherwise triggered by the same Threshold Definition. Any number of Context Attributes can be specified for a Threshold Definition (including zero). Any attribute within an Arecibo monitored event can be used as a Context Attribute. The most common example of a Context Attribute is 'hostName'. This allows specifying a single Threshold Definition that can be used to monitor a single metric for any number of individual hosts separately. Since it is by far the most common use case, the form for creating new Threshold Definitions is pre-populated with 'hostName', but this is not a requirement.</p>

    <p>There are several Options that can be specified for a Threshold Definition.</p>

    <p>Min &amp; Max Threshold: These are the values that must violated in order to trigger an alert. At least one of (or both) Min and Max Thresholds must be specified. The thresholds must be numeric. Thresholds are interpreted as non-inclusive limits.</p>

    <p>The Min Samples option provides for a minimum number of samples required to trigger an alert (the default being 1). This allows requiring multiple samples that exceed the specified threshold, within the specified Max Sample Window. This can guard against spurious noise in the event data.</p>

    <p>The Clearing Interval is a required field which indicates the minimum amount of time required to pass before a triggered alert will become de-activated, and no longer in an alerting state. The default for this field is 300000 ms (i.e. 5 minutes).</p>
  </div>
</div>

<div class="container">
    <% if (it != null) { %>
    <table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered" id="table_thresholds">
        <thead>
        <tr>
            <th>Threshold name</th>
            <th>Event type</th>
            <th>Attribute type</th>
            <th>Qualifying attributes</th>
            <th>Context attributes</th>
            <th>Options</th>
            <th>Alerting config</th>
        </tr>
        </thead>
        <tbody>
        <% for (final ThresholdDefinition thresholdDefinition : it.getThresholdConfigs()) { %>
        <tr>
            <td><%= thresholdDefinition.getThresholdDefinitionName() %>
            </td>
            <td><%= thresholdDefinition.getMonitoredEventType() %>
            </td>
            <td><%= thresholdDefinition.getMonitoredAttributeType() %>
            </td>
            <td>
                <ul>
                    <% final Iterable<ThresholdQualifyingAttr> qualifyingAttrIterable = it.getThresholdQualifyingAttrsForThresholdConfig().get(thresholdDefinition.getThresholdDefinitionName());
                        if (qualifyingAttrIterable != null) {
                            for (final ThresholdQualifyingAttr qualifyingAttr : qualifyingAttrIterable) { %>
                    <li><%= qualifyingAttr.getAttributeType() %> <i><%= qualifyingAttr.getAttributeValue() %>
                    </i></li>
                    <% }
                    } %></ul>
            </td>
            <td><%= it.getThresholdContextAttrsForThresholdConfig().get(thresholdDefinition.getThresholdDefinitionName()) %>
                <ul>
                    <%
                        final Iterable<ThresholdContextAttr> contextAttrIterable = it.getThresholdContextAttrsForThresholdConfig().get(thresholdDefinition.getThresholdDefinitionName());
                        if (contextAttrIterable != null) {
                            for (final ThresholdContextAttr qualifyingAttr : contextAttrIterable) { %>
                    <li><%= qualifyingAttr.getAttributeType() %>
                    </li>
                    <% }
                    } %></ul>
            </td>
            <td>
                <ul>
                    <li><em>Min Threshold:</em><%= thresholdDefinition.getMinThresholdValue() == null ? "<i>None</i>" : thresholdDefinition.getMinThresholdValue() %></li>
                    <li><em>Max Threshold:</em><%= thresholdDefinition.getMaxThresholdValue() == null ? "<i>None</i>" : thresholdDefinition.getMaxThresholdValue() %></li>
                    <li><em># samples:</em><%= thresholdDefinition.getMinThresholdSamples() %></li>
                    <li><em>Sample window:</em><%= thresholdDefinition.getMaxSampleWindowMs() %></li>
                    <li><em>Clearing interval:</em><%= thresholdDefinition.getClearingIntervalMs() %></li>
                </ul>
            </td>
            <td><%
                final AlertingConfig alertingConfig = it.getAlertingConfigsForThresholdConfig().get(thresholdDefinition.getThresholdDefinitionName());
                if (alertingConfig != null) { %>
                <%= alertingConfig.getAlertingConfigurationName() %>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
        <tfoot>
        <tr>
            <th>Threshold name</th>
            <th>Event type</th>
            <th>Attribute type</th>
            <th>Qualifying attributes</th>
            <th>Context attributes</th>
            <th>Options</th>
            <th>Alerting config</th>
        </tr>
        </tfoot>
    </table>
    <% } %>
</div>

<%@include file="../global_includes/footer.jsp" %>