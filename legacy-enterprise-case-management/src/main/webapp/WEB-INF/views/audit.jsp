<%--
    Business case: Let authorized reviewers inspect recorded actions to trace case
    and account changes and support accountability.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>
<bean:define id="format_timestamp"><bean:message key="format.timestamp"/></bean:define>
<h1><bean:message key="ui.audit.trail"/></h1>

<section class="panel card card-body">
    <div class="table-container">
        <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.audit.trail'/>">
            <table class="table table-striped table-hover align-middle">
                <caption class="visually-hidden"><bean:message key="ui.audit.trail"/></caption>
                <thead><tr>
                    <th scope="col"><bean:message key="ui.timestamp"/></th>
                    <th scope="col"><bean:message key="ui.actor"/></th>
                    <th scope="col"><bean:message key="ui.record"/></th>
                    <th scope="col"><bean:message key="ui.action"/></th>
                    <th scope="col"><bean:message key="ui.details"/></th>
                </tr></thead>
                <tbody>
                <c:forEach var="event" items="${auditForm.events}">
                    <tr>
                        <td><fmt:formatDate value="${event.createdAt}" pattern="${format_timestamp}"/></td>
                        <td><c:out value="${event.actor}"/></td>
                        <td><c:out value="${event.entityType}"/> #<c:out value="${event.entityId}"/></td>
                        <td><c:out value="${event.actionName}"/></td>
                        <td><c:out value="${event.details}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <nav aria-label="<bean:message key='ui.pagination'/>">
        <ul class="pagination mb-0">
            <c:if test="${auditForm.pageNumber gt 0}">
                <li class="page-item">
                    <a class="page-link" href="${pageContext.request.contextPath}${viewOptions.routes.AUDIT}?pageNumber=${auditForm.pageNumber - 1}">
                        <bean:message key="ui.previous"/>
                    </a>
                </li>
            </c:if>
            <li class="page-item active">
                <span class="page-link" aria-current="page">
                    <bean:message key="ui.page.number" arg0="${auditForm.pageNumber+1}"/>
                </span>
            </li>
            <li class="page-item">
                <a class="page-link"
                   href="${pageContext.request.contextPath}${viewOptions.routes.AUDIT}?pageNumber=${auditForm.pageNumber + 1}">
                    <bean:message key="ui.next.page"/>
                </a>
            </li>
        </ul>
    </nav>
</section>
