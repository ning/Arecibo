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
        $("#people_form").collapse();
        $("#people_form").toggle();
        $("#add_form").click(function ()
        {
            $("#people_form").toggle();
        });

        $("#navbar_people").attr("class", "active");

        $("#is_group_alias").click(function ()
        {
            $("#people_group_first_name").toggle();
            $("#people_group_last_name").toggle();
        });

        $('#table_people').dataTable({
            "sDom": "<'row'<'span6'l><'span6'f>r>t<'row'<'span6'i><'span6'p>>",
            "sPaginationType": "bootstrap",
            "oLanguage": {
                "sLengthMenu": "_MENU_ records per page"
            }
        });
    });
</script>

<div class="page-header">
    <h1>People and Aliases <i class="icon-plus" id="add_form" style="cursor: pointer;"></i></h1>
</div>

<div class="container" id="people_form">
  <form class="form-horizontal" action="/people" method="post" name="people_form">
    <fieldset>
      <legend>Add a Person or Alias</legend>

      <div class="control-group">
        <p>There are 2 types of contacts that can receive emails, either an individual Person or an Email Group Alias. We want to know if a contact is a Person or not, since it will be used as part of the Alert Acknowledgement system. Only Persons will be allowed to acknowledge alerts. A Person or Alias can be associated with a Notification Group.</p>

        <p>There are 2 types of Notification Type, either by Email or else by SMS via Email. The difference, is that for the SMS via Email, a reduced version of the notification email will be sent, limited to 140 characters, to conform to the limitation of SMS text messaging systems.</p>
      </div>

      <div class="control-group" id="is_group_alias">
        <label class="control-label" for="is_group">Group alias?</label>
        <div class="controls">
          <label class="checkbox">
            <input type="checkbox" name="is_group" id="is_group">
          </label>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="nick_name">Nick name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="nick_name" id="nick_name">
          <p class="help-block">Short identifier for this person or group. Needs to be unique. Required.</p>
        </div>
      </div>

      <div class="control-group" id="people_group_first_name">
        <label class="control-label" for="first_name">First name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="first_name" id="first_name">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group" id="people_group_last_name">
        <label class="control-label" for="last_name">Last name</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="last_name" id="last_name">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="email">Email address</label>
        <div class="controls">
          <input type="text" class="input-xlarge" name="email" id="email">
          <p class="help-block">Required.</p>
        </div>
      </div>

      <div class="control-group">
        <label class="control-label" for="notification_type">Notification type</label>
        <div class="controls">
          <select name="notification_type" id="notification_type">
            <option value="REGULAR_EMAIL">Email</option>
            <option value="SMS_VIA_EMAIL">SMS via Email</option>
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
    <jsp:useBean id="it"
                 type="Iterable<Map<String, Object>>"
                 scope="request">
    </jsp:useBean>
    <% if (it != null) { %>
    <table cellpadding="0" cellspacing="0" border="0" class="table table-striped table-bordered" id="table_people">
        <thead>
        <tr>
            <th>Nick name</th>
            <th>First name</th>
            <th>Last name</th>
            <th>Email</th>
        </tr>
        </thead>
        <tbody>
        <% for (final Map<String, Object> person : it) { %>
        <tr>
            <td><%= person.get("label") %>
            </td>
            <td><%= person.get("first_name") %>
            </td>
            <td><%= person.get("last_name") %>
            </td>
            <td><%= person.get("emails") %>
            </td>
        </tr>
        <% } %>
        </tbody>
        <tfoot>
        <tr>
            <th>Nick name</th>
            <th>First name</th>
            <th>Last name</th>
            <th>Email</th>
        </tr>
        </tfoot>
    </table>
    <% } %>
</div>

<%@include file="../global_includes/footer.jsp" %>