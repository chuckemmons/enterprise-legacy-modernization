<%--
    Business case: Show users case-related notices and let them mark notices as read to track updates requiring attention.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>
<bean:define id="format_datetime"><bean:message key="format.datetime"/></bean:define>

<h1><bean:message key="ui.notifications"/></h1>

<section class="panel card card-body">
    <c:forEach var="n" items="${notificationForm.notices}">
        <div class="entry">
            <div class="between d-flex flex-wrap align-items-sm-center justify-content-between gap-3">
                <strong><c:out value="${n.subject}"/></strong>
                <small><fmt:formatDate value="${n.createdAt}" pattern="${format_datetime}"/></small>
            </div>

            <p><c:out value="${n.message}"/></p>

            <c:if test="${not empty n.caseId}">
                <a href="${pageContext.request.contextPath}${viewOptions.routes.CASE_VIEW}?id=${n.caseId}">
                    <bean:message key="ui.open.related.case"/>
                </a>
            </c:if>

            <c:if test="${not n.read}">
                <form action="${pageContext.request.contextPath}${viewOptions.routes.NOTIFICATION_READ}" method="post">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${n.id}">
                    <br>
                    <button class="minor btn btn-outline-secondary btn-sm"><bean:message key="ui.mark.read"/></button>
                </form>
            </c:if>

        </div>
    </c:forEach>

    <c:if test="${empty notificationForm.notices}"><p><bean:message key="ui.no.notifications.yet"/></p></c:if>
</section>
