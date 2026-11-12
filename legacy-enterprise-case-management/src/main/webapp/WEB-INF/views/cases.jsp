<%--
    Business case: Help staff locate service cases by search, status, and assignee,
    review results, and start a new case.

    Author: Chuck Emmons
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@ include file="fragments/taglibs.jspf" %>
<bean:define id="format_date"><bean:message key="format.date"/></bean:define>
<bean:define id="unassignedLabel"><bean:message key="ui.unassigned"/></bean:define>

<div class="between d-flex flex-wrap align-items-sm-center justify-content-between gap-3">
    <h1><bean:message key="ui.cases"/></h1>

    <c:if test="${sessionScope[viewOptions.keys.ROLE] ne viewOptions.roleCodes.AUDITOR}">
        <a class="button btn btn-primary" href="${pageContext.request.contextPath}${viewOptions.routes.CASE_NEW}">
            <bean:message key="ui.create.case"/>
        </a>
    </c:if>
</div>

<section class="panel card card-body"><h2><bean:message key="ui.search.and.filter"/></h2>
    <form method="get" action="${pageContext.request.contextPath}${viewOptions.routes.CASES}"
          class="filters d-flex flex-wrap align-items-end gap-3">
        <label class="form-label d-block mb-3" for="case-filter-q">
            <fmt:message key="ui.search.title.or.id"/>
            <input aria-describedby="case-filter-q-error" class="form-control" id="case-filter-q" type="search" name="q"
                   maxlength="${viewOptions.limits.SEARCH}" value="<c:out value='${caseForm.q}'/>"
                   placeholder="<bean:message key='ui.case.number.or.title'/>">
        </label>
        <div id="case-filter-q-error" class="field-errors">
            <html:messages id="qError" property="q">
                <p class="invalid-feedback d-block"><c:out value="${qError}"/></p>
            </html:messages>
        </div>

        <label class="form-label d-block mb-3" for="case-filter-status">
            <fmt:message key="ui.status"/>
            <select aria-describedby="case-filter-status-error" class="form-select" id="case-filter-status" name="status">
                <option value=""><fmt:message key="ui.all"/></option>
                <c:forEach items="${viewOptions.statuses}" var="opt">
                    <option value="${opt}" <c:if test="${caseForm.status eq opt}">selected</c:if>><c:out value="${opt}"/></option>
                </c:forEach>
            </select>
        </label>
        <div id="case-filter-status-error" class="field-errors">
            <html:messages id="statusError" property="status">
                <p class="invalid-feedback d-block"><c:out value="${statusError}"/></p>
            </html:messages>
        </div>

        <label class="form-label d-block mb-3" for="case-filter-assignee">
            <fmt:message key="ui.assigned.user"/>
            <input aria-describedby="case-filter-assignee-error" class="form-control" id="case-filter-assignee"
                   name="assignee" value="<c:out value='${caseForm.assignee}'/>" maxlength="${viewOptions.limits.USERNAME}">
        </label>
        <div id="case-filter-assignee-error" class="field-errors">
            <html:messages id="assigneeError" property="assignee">
                <p class="invalid-feedback d-block"><c:out value="${assigneeError}"/></p>
            </html:messages>
        </div>

        <button class="btn btn-primary"><bean:message key="ui.search"/></button>
    </form>
</section>

<section class="panel card card-body">
    <div class="table-container">
        <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.cases'/>">
        <table class="table table-striped table-hover align-middle">
            <caption class="visually-hidden"><bean:message key="ui.cases"/></caption>
            <thead>
            <tr>
                <th scope="col"><bean:message key="ui.case"/></th>
                <th scope="col"><bean:message key="ui.title"/></th>
                <th scope="col"><bean:message key="ui.priority"/></th>
                <th scope="col"><bean:message key="ui.status"/></th>
                <th scope="col"><bean:message key="ui.assigned.to.heading"/></th>
                <th scope="col"><bean:message key="ui.updated"/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${caseForm.cases}" var="c">
                <tr>
                    <td>
                        <a href="${pageContext.request.contextPath}${viewOptions.routes.CASE_VIEW}?id=${c.id}">
                            <c:out value="${c.caseNumber}"/>
                        </a>
                    </td>
                    <td><c:out value="${c.title}"/></td>
                    <td><c:out value="${c.priority}"/></td>
                    <td><c:out value="${c.status}"/></td>
                    <td><c:out value="${c.assignedTo}" default="${unassignedLabel}"/></td>
                    <td><fmt:formatDate value="${c.updatedAt}" pattern="${format_date}"/></td>
                </tr>
            </c:forEach>

            <c:if test="${empty caseForm.cases}">
                <tr>
                    <td colspan="6"><bean:message key="ui.no.matching.cases"/></td>
                </tr>
            </c:if>
            </tbody>
        </table>
        </div>
    </div>

    <c:url var="previousPage" value="${viewOptions.routes.CASES}">
        <c:param name="pageNumber" value="${caseForm.pageNumber - 1}"/>
        <c:param name="q" value="${caseForm.q}"/>
        <c:param name="status" value="${caseForm.status}"/>
        <c:param name="assignee" value="${caseForm.assignee}"/>
    </c:url>

    <c:url var="nextPage" value="${viewOptions.routes.CASES}">
        <c:param name="pageNumber" value="${caseForm.pageNumber + 1}"/>
        <c:param name="q" value="${caseForm.q}"/>
        <c:param name="status" value="${caseForm.status}"/>
        <c:param name="assignee" value="${caseForm.assignee}"/>
    </c:url>

    <nav aria-label="<bean:message key='ui.pagination'/>">
        <ul class="pagination mb-0">
            <c:if test="${caseForm.pageNumber gt 0}">
                <li class="page-item">
                    <a class="page-link" href="<c:out value='${previousPage}'/>"><bean:message key="ui.previous"/></a>
                </li>
            </c:if>

            <li class="page-item active">
                <span class="page-link" aria-current="page">
                    <bean:message key="ui.page.number" arg0="${caseForm.pageNumber+1}"/>
                </span>
            </li>

            <c:if test="${not empty caseForm.cases and fn:length(caseForm.cases) eq 25}">
                <li class="page-item">
                    <a class="page-link" href="<c:out value='${nextPage}'/>"><bean:message key="ui.next"/></a>
                </li>
            </c:if>
        </ul>
    </nav>
</section>
