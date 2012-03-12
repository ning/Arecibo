<%@ page import="java.util.Map" %>
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
    $(document).ready(function()
    {
        $("#navbar_groups").attr("class", "active");

        $("#groups_form").collapse();
        $("#groups_form").toggle();
        $("#add_form").click(function ()
        {
            $("#groups_form").toggle();
        });

        $('#table_groups').dataTable({
            "sDom": "<'row'<'span6'l><'span6'f>r>t<'row'<'span6'i><'span6'p>>",
            "sPaginationType": "bootstrap",
            "oLanguage": {
                "sLengthMenu": "_MENU_ records per page"
            }
        });
    });
</script>

<div class="page-header">
    <h1>Notification groups <i class="icon-plus" id="add_form" style="cursor: pointer;"></i></h1>
</div>

<jsp:useBean id="it"
             type="com.ning.arecibo.alertmanager.models.NotificationGroupsModel"
             scope="request">
</jsp:useBean>

<div class="container" id="groups_form">
  <form class="form-horizontal" action="/ui/groups" method="post" name="groups_form">
    <fieldset>
      <legend>Add a Notification group</legend>

      <div class="control-group">
        <p>A Notification Group is a way for grouping multiple recipients to receive the same alert notification. A Notification Group can be associated with one or more Alerting Configurations. A Notification Group must include at least one email contact, which can be for an individual person or an email alias. It may be necessary to set up a Person or Alias before creating a Threshold Definition.</p>
      </div>

      <div class="control-group">
        <label class="control-label" for="group_name">Group name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="group_name" id="group_name">
          <p class="help-block">Short identifier for this notification group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="is_group_enabled">Notifications enabled?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" name="is_group_enabled" id="is_group_enabled" value="true">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="person_or_alias">People and/or Aliases</label>
        <div class="controls">
            <select multiple="multiple" name="person_or_alias" id="person_or_alias">
                <% if (it != null) {
                    for (final Map<String, Object> person : it.getAllPeopleAndGroups()) { %>
                    <option value="<%= person.get("id") %>"><%= person.get("label") %></option>
                <% } } %>
            </select>
        </div>
      </div>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary">Save changes</button>
      </div>
    </fieldset>
  </form>
</div>

<div class="container">
    <% if (it != null) { %>
    <table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered" id="table_groups">
        <thead>
        <tr>
            <th>Group name</th>
            <th>Email</th>
            <th>Enabled</th>
        </tr>
        </thead>
        <tbody>
        <% for (final Map<String, String> group : it.getExistingNotificationGroups()) { %>
        <tr>
            <td><%= group.get("label") %>
            </td>
            <td><%= group.get("emails") %>
            </td>
            <td><%= group.get("enabled") %>
            </td>
        </tr>
        <% } %>
        </tbody>
        <tfoot>
        <tr>
            <th>Group name</th>
            <th>Email</th>
            <th>Enabled</th>
        </tr>
        </tfoot>
    </table>
    <% } %>
</div>

<%@include file="../global_includes/footer.jsp" %>