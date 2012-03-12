<%@include file="../global_includes/header.jsp" %>

<%@include file="../global_includes/navbar.jsp" %>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_thresholds").attr("class", "active");
    });
</script>

<div class="container">
  <form class="form-horizontal">
    <fieldset>
      <legend>Threshold Definitions</legend>

      <div class="control-group">
        <p>A Threshold Definition is a specific rule for defining conditions that should trigger an alert. It is specified by choosing an Event Type and an associated Attribute Type. These must correspond to valid, monitored events and attributes within the Arecibo system, in order to be triggered. These types are case sensitive, so be sure to spell them correctly, observing case. You can refer to the Arecibo Dashboard to view currently active monitored events, for spelling. The specified attribute type must be a numeric valued field.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="threshold_name">Threshold name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="threshold_name">
          <p class="help-block">Short identifier for this threshold. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="event_name">Event name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="event_name">
          <p class="help-block">Usually JMX Bean. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="attribute_name">Attribute name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="attribute_name">
          <p class="help-block">Usually JMX attribute. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="alerting_configuration">Alerting configuration</label>
        <div class="controls">
          <select id="alerting_configuration">
          </select>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="min_threshold">Minimum threshold</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="min_threshold">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="max_threshold">Maximum threshold</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="max_threshold">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="min_samples">Minimum samples</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="min_samples">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="max_samples">Maximum samples window (ms)</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="max_samples">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="clearing_interval">Clearing interval (ms)</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="clearing_interval">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="qualifying_attribute_type">Qualifying Attribute Type</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="qualifying_attribute_type">
        </div>
      </div>
      <div class="control-group">
        <label class="control-label" for="qualifying_attribute_value">Qualifying Attribute Value</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="qualifying_attribute_value">
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="context_attribute_type">Context Attribute Type</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="context_attribute_type">
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
</div><!--/.fluid-container-->

<%@include file="../global_includes/footer.jsp" %>