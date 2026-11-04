<%@ include file="fragments/header.jspf" %><section class="panel"><h1>Request could not be completed</h1>
<p><c:out value="${requestScope.error}" default="Unexpected server error. Review the application logs."/></p>
<a href="${pageContext.request.contextPath}/dashboard.do">Return to dashboard</a></section>
<%@ include file="fragments/footer.jspf" %>
