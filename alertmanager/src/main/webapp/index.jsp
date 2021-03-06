<%@include file="./global_includes/header.jsp" %>

<%@include file="./global_includes/navbar.jsp" %>

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
        $("#navbar_home").attr("class", "active");
    });
</script>

<div class="container">
  <div class="hero-unit">
    <h2>Welcome to the Arecibo Alert Manager!</h2>
    <p>From here you can configure alert definitions and thresholds, as well as notifications and email aliases.</p>
  </div>
</div>

<%@include file="./global_includes/footer.jsp" %>