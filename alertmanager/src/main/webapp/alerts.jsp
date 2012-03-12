<%@include file="./global_includes/header.jsp" %>

<%@include file="./global_includes/navbar.jsp" %>

<script type="text/javascript">
    $(document).ready(function() {
        $("#navbar_alerts").attr("class", "active");
    });
</script>

<div class="container">
  <p>Manage rules for suppressing alerts, either by regular schedule, or by manual override.</p>
</div><!--/.fluid-container-->

<%@include file="./global_includes/footer.jsp" %>