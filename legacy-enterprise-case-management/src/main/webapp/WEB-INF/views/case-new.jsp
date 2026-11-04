<%@ include file="fragments/header.jspf" %><h1>Open a case</h1><section class="panel">
<form action="${pageContext.request.contextPath}/caseCreate.do" method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}">
<label>Title<input name="title" maxlength="180" required></label>
<label>Description<textarea name="description" rows="6" maxlength="4000" required></textarea></label>
<div class="cols"><label>Category<input name="category" maxlength="60" value="GENERAL" required></label>
<label>Priority<select name="priority"><option>LOW</option><option selected>MEDIUM</option><option>HIGH</option><option>CRITICAL</option></select></label></div>
<div class="cols"><label>Department<input name="department" maxlength="80" value="Operations" required></label>
<label>Due date<input type="date" name="due"></label></div>
<button>Create case</button> <a href="${pageContext.request.contextPath}/cases.do">Cancel</a>
</form></section><%@ include file="fragments/footer.jspf" %>
