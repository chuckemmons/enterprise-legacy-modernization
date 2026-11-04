<%@ include file="fragments/header.jspf" %><h1>Operational reports</h1><p>Basic legacy counts; advanced interactive analytics are reserved for modernization.</p>
<div class="cols"><section class="panel"><h2>By status</h2><table><tr><th>Status</th><th>Total</th></tr><c:forEach var="r" items="${statusRows}"><tr><td><c:out value="${r[0]}"/></td><td><c:out value="${r[1]}"/></td></tr></c:forEach></table></section>
<section class="panel"><h2>By priority</h2><table><tr><th>Priority</th><th>Total</th></tr><c:forEach var="r" items="${priorityRows}"><tr><td><c:out value="${r[0]}"/></td><td><c:out value="${r[1]}"/></td></tr></c:forEach></table><p>Overdue: <strong><c:out value="${overdue}"/></strong></p></section></div>
<a class="button" href="${pageContext.request.contextPath}/reportsExport.do">Export CSV (up to 5,000 rows)</a>
<%@ include file="fragments/footer.jspf" %>
