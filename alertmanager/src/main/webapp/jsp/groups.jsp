<%@include file="../global_includes/header.jsp" %>

<%@include file="../global_includes/navbar.jsp" %>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_groups").attr("class", "active");
    });
</script>

<div class="container">
  <form class="form-horizontal">
    <fieldset>
      <legend>Notification group</legend>

      <div class="control-group">
        <p>A Notification Group is a way for grouping multiple recipients to receive the same alert notification. A Notification Group can be associated with one or more Alerting Configurations. A Notification Group must include at least one email contact, which can be for an individual person or an email alias. It may be necessary to set up a Person or Alias before creating a Threshold Definition.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="group_name">Group name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="group_name">
          <p class="help-block">Short identifier for this notification group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group" id="is_group_enabled">
        <label class="control-label" for="is_group_enabled">Notifications enabled?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" id="is_group_enabled" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="person_or_alias">People and/or Aliases</label>
        <div class="controls">
          <select multiple="multiple" id="person_or_alias">
          </select>
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save changes</button>
      </div>
    </fieldset>
  </form>
</div><!--/.fluid-container-->

<%@include file="../global_includes/footer.jsp" %>