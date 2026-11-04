<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ include file="fragments/header.jspf" %><div class="between"><h1>Cases</h1>
<c:if test="${sessionScope.role ne 'AUDITOR'}"><a class="button" href="${pageContext.request.contextPath}/caseNew.do">Create case</a></c:if></div>
<section class="panel"><h2>Search and filter</h2><form method="get" action="${pageContext.request.contextPath}/cases.do" class="filters">
<label>Search title or ID<input type="search" name="q" maxlength="80" value="<c:out value='${q}'/>" placeholder="Case number or title"></label>
<label>Status<select name="status"><option value="">All</option>
<c:forTokens items="NEW,TRIAGED,IN_PROGRESS,PENDING_APPROVAL,APPROVED,REJECTED,CLOSED" delims="," var="opt">
<option value="${opt}" <c:if test="${status eq opt}">selected</c:if>><c:out value="${opt}"/></option></c:forTokens>
</select></label><label>Assigned user<input name="assignee" value="<c:out value='${assignee}'/>" maxlength="60"></label><button>Search</button></form></section>
<section class="panel"><div class="scroll"><table><thead><tr><th>Case #</th><th>Title</th><th>Priority</th><th>Status</th><th>Assigned to</th><th>Updated</th></tr></thead><tbody>
<c:forEach items="${cases}" var="c"><tr>
<td><a href="${pageContext.request.contextPath}/caseView.do?id=${c.id}"><c:out value="${c.caseNumber}"/></a></td>
<td><c:out value="${c.title}"/></td><td><c:out value="${c.priority}"/></td><td><c:out value="${c.status}"/></td>
<td><c:out value="${c.assignedTo}" default="Unassigned"/></td><td><fmt:formatDate value="${c.updatedAt}" pattern="yyyy-MM-dd"/></td></tr></c:forEach>
<c:if test="${empty cases}"><tr><td colspan="6">No matching cases.</td></tr></c:if>
</tbody></table></div><div class="between"><c:if test="${page gt 0}"><a href="${pageContext.request.contextPath}/cases.do?page=${page - 1}">Previous</a></c:if>
<span>Page ${page+1}</span><c:if test="${not empty cases and fn:length(cases) eq 25}"><a href="${pageContext.request.contextPath}/cases.do?page=${page + 1}">Next</a></c:if></div></section>
<%@ include file="fragments/footer.jspf" %>
