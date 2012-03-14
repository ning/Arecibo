<%@ page import="com.google.common.base.Strings" %>
<%@ page import="com.ning.arecibo.alert.confdata.AlertingConfig" %>
<%@ page import="com.ning.arecibo.alert.confdata.NotifGroup" %>
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
        $("#navbar_alerting_config").attr("class", "active");

        $("#alerting_configs_form").collapse();
        $("#alerting_configs_form").toggle();
        $("#add_form").click(function ()
        {
            $("#alerting_configs_form").toggle();
        });

        $('#table_alerting_configs').dataTable({
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
    <h1>Alerting Configurations <i class="icon-plus" id="add_form" style="cursor: pointer;"></i></h1>
  </div>
</div>

<jsp:useBean id="it"
             type="com.ning.arecibo.alertmanager.models.AlertingConfigurationsModel"
             scope="request">
</jsp:useBean>

<div class="container" id="alerting_configs_form">
  <form class="form-horizontal" action="/ui/alerting" method="post" name="alerting_configs_form">
    <fieldset>
      <legend>Add an Alerting Configuration</legend>

      <div class="control-group">
        <p>An Alerting configuration allows defining notification options, that can be shared by one or more Threshold Definitions. Thus, Alerting Configurations allow managing groups of alerts with a single configuration.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="config_name">Alerting configuration name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="config_name" id="config_name">
          <p class="help-block">Short identifier for this alerting configuration group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="is_config_enabled">Configuration enabled?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" name="is_config_enabled" id="is_config_enabled" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notify_on_recovery">Notify on recovery?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" name="notify_on_recovery" id="notify_on_recovery" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="repeat_mode">Repeat mode</label>
        <div class="controls">
          <select name="repeat_mode" id="repeat_mode">
            <option>NO_REPEAT</option>
            <option>UNTIL_CLEARED</option>
          </select>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notification_groups">Notification groups</label>
        <div class="controls">
          <select multiple="multiple" name="notification_groups" id="notification_groups">
              <% if (it != null) {
                  for (final NotifGroup notificationGroup : it.getAllNotificationGroups()) { %>
              <option value="<%= notificationGroup.getId() %>"><%= notificationGroup.getGroupName() %>
              </option>
              <% }
              } %>
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
</div>

<div class="container">
    <% if (it != null) { %>
    <table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered" id="table_alerting_configs">
        <thead>
        <tr>
            <th>Alerting Config name</th>
            <th>Repeat mode</th>
            <th>Repeat interval</th>
            <th>Notify on recovery?</th>
            <th>Notification groups</th>
            <th>Enabled</th>
        </tr>
        </thead>
        <tbody>
        <% for (final AlertingConfig configuration : it.getAlertingConfigurations()) { %>
        <tr>
            <td><%= configuration.getAlertingConfigurationName() %>
            </td>
            <td><%= Strings.nullToEmpty(configuration.getRepeatMode()) %>
            </td>
            <td><%= configuration.getRepeatInterval() == null ? "" : configuration.getRepeatInterval() %>
            </td>
            <td><%= configuration.isNotifyOnRecovery() %>
            </td>
            <td><%
                final Iterable<NotifGroup> notificationGroups = it.getNotificationsGroupsForAlertingConfig().get(configuration.getAlertingConfigurationName());
                if (notificationGroups != null) {
                    for (final NotifGroup notifGroup : notificationGroups) { %>
                <li><%= Strings.nullToEmpty(notifGroup.getGroupName()) %>
                </li>
                <% }
                } %>
            </td>
            <td><%= configuration.isEnabled() %>
            </td>
        </tr>
        <% } %>
        </tbody>
        <tfoot>
        <tr>
            <th>Alerting Config name</th>
            <th>Repeat mode</th>
            <th>Repeat interval</th>
            <th>Notify on recovery?</th>
            <th>Notification groups</th>
            <th>Enabled</th>
        </tr>
        </tfoot>
    </table>
    <% } %>
</div>

<%@include file="../global_includes/footer.jsp" %>