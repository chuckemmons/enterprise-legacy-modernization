<%@ include file="fragments/header.jspf" %><h1>User administration</h1><section class="panel"><h2>Create account</h2>
<form action="${pageContext.request.contextPath}/userCreate.do" method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}">
<div class="cols"><label>Username<input name="username" required maxlength="60"></label><label>Initial password (12+ chars)<input type="password" name="password" minlength="12" required></label></div>
<div class="cols"><label>Name<input name="name" required maxlength="120"></label><label>Email<input type="email" name="email" maxlength="254"></label></div>
<div class="cols"><label>Department<input name="department" value="Operations" required maxlength="80"></label><label>Role<select name="role"><option>WORKER</option><option>SUPERVISOR</option><option>AUDITOR</option><option>ADMIN</option></select></label></div>
<button>Create user</button></form></section><section class="panel"><h2>Accounts</h2><div class="scroll"><table><tr><th>User</th><th>Name</th><th>Role</th><th>Department</th><th>Enabled</th><th>Manage</th></tr>
<c:forEach var="u" items="${users}"><tr><td><c:out value="${u.username}"/></td><td><c:out value="${u.displayName}"/></td><td><c:out value="${u.role}"/></td>
<td><c:out value="${u.department}"/></td><td><c:out value="${u.enabled}"/></td><td>
<c:if test="${u.username ne sessionScope.actor}">
<form method="post" action="${pageContext.request.contextPath}/userRole.do" class="inline"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${u.id}"><select name="role"><c:forTokens var="v" items="ADMIN,SUPERVISOR,WORKER,AUDITOR" delims=","><option <c:if test="${v eq u.role}">selected</c:if>><c:out value="${v}"/></option></c:forTokens></select><button class="minor">Role</button></form>
<form method="post" action="${pageContext.request.contextPath}/userDisable.do" class="inline"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${u.id}"><button class="minor">Toggle active</button></form>
</c:if></td></tr></c:forEach></table></div></section><%@ include file="fragments/footer.jspf" %>
