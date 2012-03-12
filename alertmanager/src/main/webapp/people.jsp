<%@include file="./global_includes/header.jsp" %>

<%@include file="./global_includes/navbar.jsp" %>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_people").attr("class", "active");

        $("#is_group_alias").click(function () {
          $("#people_group_first_name").toggle();
          $("#people_group_last_name").toggle();
        });
    });
</script>

<div class="container">
  <form class="form-horizontal">
    <fieldset>
      <legend>Person or Alias</legend>

      <div class="control-group">
        <p>There are 2 types of contacts that can receive emails, either an individual Person or an Email Group Alias. We want to know if a contact is a Person or not, since it will be used as part of the Alert Acknowledgement system. Only Persons will be allowed to acknowledge alerts. A Person or Alias can be associated with a Notification Group.</p>

        <p>There are 2 types of Notification Type, either by Email or else by SMS via Email. The difference, is that for the SMS via Email, a reduced version of the notification email will be sent, limited to 140 characters, to conform to the limitation of SMS text messaging systems.</p>
      </div>

      <div class="control-group" id="is_group_alias">
        <label class="control-label" for="is_group">Group alias?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" id="is_group" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="nick_name">Nick name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="nick_name">
          <p class="help-block">Short identifier for this person or group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group" id="people_group_first_name">
        <label class="control-label" for="first_name">First name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="first_name">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group" id="people_group_last_name">
        <label class="control-label" for="last_name">Last name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="last_name">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="email">Email address</label>
        <div class="controls">
          <input type="text" class="input-xlarge" id="email">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notification_type">Notification type</label>
        <div class="controls">
          <select id="notification_type">
            <option>Email</option>
            <option>SMS via Email</option>
          </select>
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save changes</button>
      </div>
    </fieldset>
  </form>
</div><!--/.fluid-container-->

<%@include file="./global_includes/footer.jsp" %>