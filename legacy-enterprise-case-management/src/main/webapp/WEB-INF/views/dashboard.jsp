<%--
    Business case: Give signed-in staff an overview of overdue cases and workload so they can
    identify work needing attention.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>

<h1><bean:message key="ui.work.dashboard"/></h1>

<p>
    <bean:message key="ui.welcome"/>
    <strong><c:out value="${sessionScope[viewOptions.keys.DISPLAY_NAME]}"/></strong>
    <bean:message key="ui.signed.in.as"/>
    <c:out value="${sessionScope[viewOptions.keys.ROLE]}"/>.
</p>

<div class="row g-4 mb-4">
    <div class="col-12 col-lg-6">
        <article class="panel card card-body h-100">
            <div class="kpi"><c:out value="${dashboardForm.summary.overdue}"/></div>
            <h2><bean:message key="ui.overdue.cases"/></h2>
        </article>
    </div>
    <div class="col-12 col-lg-6">
        <article class="panel card card-body h-100">
            <h2><bean:message key="ui.case.workload"/></h2>
            <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.case.workload'/>">
                <table class="table table-striped table-hover align-middle">
                    <caption class="visually-hidden"><bean:message key="ui.case.workload"/></caption>
                    <thead>
                        <tr>
                            <th scope="col"><bean:message key="ui.status"/></th>
                            <th scope="col"><bean:message key="ui.count"/></th>
                        </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${dashboardForm.summary.statuses}" var="r">
                        <tr>
                            <td><c:out value="${r.label}"/></td>
                            <td><c:out value="${r.total}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </article>
    </div>
</div>

<p>
    <a class="button btn btn-primary" href="${pageContext.request.contextPath}${viewOptions.routes.CASES}">
        <bean:message key="ui.browse.cases"/>
    </a>
    <c:if test="${sessionScope[viewOptions.keys.ROLE] ne viewOptions.roleCodes.AUDITOR}">
        <a class="button btn btn-primary" href="${pageContext.request.contextPath}${viewOptions.routes.CASE_NEW}">
            <bean:message key="ui.open.a.new.case"/>
        </a>
    </c:if>
</p>
