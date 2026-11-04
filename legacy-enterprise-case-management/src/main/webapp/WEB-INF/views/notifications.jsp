<%@ include file="fragments/header.jspf" %><h1>Notifications</h1><section class="panel">
<c:forEach var="n" items="${notices}"><div class="entry"><div class="between"><strong><c:out value="${n.subject}"/></strong><small><fmt:formatDate value="${n.createdAt}" pattern="yyyy-MM-dd HH:mm"/></small></div>
<p><c:out value="${n.message}"/></p><c:if test="${not empty n.caseId}"><p><a href="${pageContext.request.contextPath}/caseView.do?id=${n.caseId}">Open related case</a></p></c:if>
<c:if test="${not n.read}"><form action="${pageContext.request.contextPath}/notificationRead.do" method="post"><input type="hidden" name="csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${n.id}"><button class="minor">Mark read</button></form></c:if></div></c:forEach>
<c:if test="${empty notices}"><p>No notifications yet.</p></c:if>
</section><%@ include file="fragments/footer.jspf" %>
