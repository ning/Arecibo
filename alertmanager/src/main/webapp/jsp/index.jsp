<%@include file="../global_includes/header.jsp" %>

<%@include file="../global_includes/navbar.jsp" %>

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

<%@include file="../global_includes/footer.jsp" %>