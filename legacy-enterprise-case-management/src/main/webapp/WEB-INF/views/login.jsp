<%@ include file="fragments/header.jspf" %>
<section class="panel narrow"><h1>Sign in</h1><p>Use your provisioned user account. Demo seed users are optional.</p>
<form action="${pageContext.request.contextPath}/authenticate.do" method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}">
<label>Username<input name="username" required maxlength="60" autocomplete="username"></label>
<label>Password<input type="password" name="password" required autocomplete="current-password"></label>
<button type="submit">Sign in</button></form></section>
<%@ include file="fragments/footer.jspf" %>
