<%@ include file="fragments/header.jspf" %><h1>Work dashboard</h1>
<p>Welcome, <strong><c:out value="${sessionScope.displayName}"/></strong>. Signed in as <c:out value="${sessionScope.role}"/>.</p>
<div class="tiles"><article class="panel"><div class="kpi"><c:out value="${overdue}"/></div><h3>Overdue cases</h3></article>
<article class="panel"><h3>Case workload</h3><table><thead><tr><th>Status</th><th>Count</th></tr></thead><tbody>
<c:forEach items="${statuses}" var="r"><tr><td><c:out value="${r[0]}"/></td><td><c:out value="${r[1]}"/></td></tr></c:forEach>
</tbody></table></article></div>
<p><a class="button" href="${pageContext.request.contextPath}/cases.do">Browse cases</a>
<c:if test="${sessionScope.role ne 'AUDITOR'}"><a class="button" href="${pageContext.request.contextPath}/caseNew.do">Open a new case</a></c:if></p>
<%@ include file="fragments/footer.jspf" %>
