<%@include file="../global_includes/header.jsp" %>

<%@include file="../global_includes/navbar.jsp" %>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_alerting_config").attr("class", "active");
    });
</script>

<div class="container">
  <form class="form-horizontal">
    <fieldset>
      <legend>Alerting Configurations</legend>

      <div class="control-group">
        <p>An Alerting configuration allows defining notification options, that can be shared by one or more Threshold Definitions. Thus, Alerting Configurations allow managing groups of alerts with a single configuration.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="config_name">Alerting configuration name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="config_name">
          <p class="help-block">Short identifier for this alerting configuration group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="is_config_enabled">Configuration enabled?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" id="is_config_enabled" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notify_on_recovery">Notify on recovery?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" id="notify_on_recovery" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="repeat_mode">Repeat mode</label>
        <div class="controls">
          <select id="repeat_mode">
            <option>NO_REPEAT</option>
            <option>UNTIL_CLEARED</option>
          </select>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notification_groups">Notification groups</label>
        <div class="controls">
          <select multiple="multiple" id="notification_groups">
          </select>
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save changes</button>
      </div>
    </fieldset>
  </form>

  <div>
    <p>An Alerting Configuration includes references to Notification Groups, which allow sending notification to multiple recipients. At least 1 Notification Group must be specified for each Alerting Configuration, so it may be necessary to set up a Notification Group before creating an Alerting Configuration.</p>
    <p>Also, Managing Rules can be specified, so that all associated Threshold Definitions can be suppressed by schedule or manually. Managing Rules are optional for Alerting Configurations.</p>
    <p>There is an option to enable an Alerting Configuration, and this must be selected in order for any alerting notification or internal state management to occur.</p>
    <p>The Repeat Mode option allows customizing how often notifications will be sent in response to a triggered alert. The allowed options include NO_REPEAT and UNTIL_CLEARED.</p>
    <p>The NO_REPEAT option indicates that only an initial notification will be sent upon initial activation of the triggered alert.</p>
    <p>The UNTIL_CLEARED option indicates that notification will be repeated until the triggered alert is no longer active. The Repeat Interval option will then be used to determine the interval between repeated notifications.</p>
    <p>The Notify On Recovery option, if enabled, causes an additional notication to be sent indicating that the alert activation has cleared.</p>
  </div>
</div><!--/.fluid-container-->

<%@include file="../global_includes/footer.jsp" %>