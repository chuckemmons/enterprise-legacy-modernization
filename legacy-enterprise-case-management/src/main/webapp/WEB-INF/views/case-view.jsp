<%@ include file="fragments/header.jspf" %>
<p><a href="${pageContext.request.contextPath}/cases.do">← Case list</a></p>
<div class="between"><h1><c:out value="${item.caseNumber}"/> — <c:out value="${item.title}"/></h1><span class="pill"><c:out value="${item.status}"/></span></div>
<p>Created by <c:out value="${item.createdBy}"/> · Department: <c:out value="${item.department}"/>
· Last updated <fmt:formatDate value="${item.updatedAt}" pattern="yyyy-MM-dd HH:mm"/> · Version <c:out value="${item.version}"/></p>
<div class="cols">
<section class="panel"><h2>Details</h2>
<c:choose><c:when test="${mayEdit}">
<form action="${pageContext.request.contextPath}/caseUpdate.do" method="post">
<input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}"><input type="hidden" name="version" value="${item.version}">
<label>Title<input name="title" required maxlength="180" value="<c:out value='${item.title}'/>"></label>
<label>Description<textarea name="description" rows="5" required maxlength="4000"><c:out value="${item.description}"/></textarea></label>
<label>Priority<select name="priority"><c:forTokens items="LOW,MEDIUM,HIGH,CRITICAL" delims="," var="v"><option <c:if test="${item.priority eq v}">selected</c:if>><c:out value="${v}"/></option></c:forTokens></select></label>
<label>Due date<input type="date" name="due" value="<fmt:formatDate value='${item.dueAt}' pattern='yyyy-MM-dd'/>"></label><button>Save changes</button></form>
</c:when><c:otherwise><p><c:out value="${item.description}"/></p><p>Priority: <c:out value="${item.priority}"/></p></c:otherwise></c:choose>
</section><section class="panel"><h2>Workflow</h2><p>Assigned to: <strong><c:out value="${item.assignedTo}" default="Unassigned"/></strong></p>
<c:if test="${sessionScope.role eq 'ADMIN' or sessionScope.role eq 'SUPERVISOR'}">
<form method="post" action="${pageContext.request.contextPath}/caseAssign.do"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}">
<label>Assign to<select name="assignee"><c:forEach var="u" items="${users}"><c:if test="${u.enabled and u.role ne 'AUDITOR'}"><option value="<c:out value='${u.username}'/>"><c:out value="${u.displayName}"/> (<c:out value="${u.username}"/>)</option></c:if></c:forEach></select></label><button>Assign</button></form></c:if>
<c:if test="${not empty nextStates}"><form method="post" action="${pageContext.request.contextPath}/caseTransition.do"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}">
<label>Next status<select name="destination"><c:forEach items="${nextStates}" var="next"><option value="${next}"><c:out value="${next}"/></option></c:forEach></select></label><button>Advance workflow</button></form></c:if>
<c:if test="${sessionScope.role eq 'ADMIN'}"><form method="post" action="${pageContext.request.contextPath}/caseArchive.do" onsubmit="return confirm('Archive this case?');"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}"><button class="danger">Archive case</button></form></c:if>
</section></div>
<div class="cols"><section class="panel"><h2>Discussion / notes</h2><c:if test="${mayComment}"><form method="post" action="${pageContext.request.contextPath}/caseComment.do"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}"><label>New note<textarea name="body" rows="3" maxlength="4000" required></textarea></label><button>Add note</button></form></c:if>
<c:forEach var="n" items="${notes}"><div class="entry"><small><c:out value="${n.author}"/> · <fmt:formatDate value="${n.createdAt}" pattern="yyyy-MM-dd HH:mm"/></small><p><c:out value="${n.body}"/></p></div></c:forEach>
</section><section class="panel"><h2>Documents (max 2 MB)</h2><c:if test="${mayEdit}">
<form method="post" action="${pageContext.request.contextPath}/caseUpload.do" enctype="multipart/form-data"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${item.id}">
<label>Attach PDF, text, JPEG, or PNG<input type="file" name="file" required></label><button>Upload document</button></form></c:if>
<ul><c:forEach var="doc" items="${docs}"><li><a href="${pageContext.request.contextPath}/caseDownload.do?id=${doc.id}"><c:out value="${doc.originalName}"/></a> (<c:out value="${doc.contentSize}"/> bytes)</li></c:forEach></ul>
</section></div>
<%@ include file="fragments/footer.jspf" %>
