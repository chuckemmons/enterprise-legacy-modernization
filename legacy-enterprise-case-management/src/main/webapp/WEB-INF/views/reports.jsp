<%--
    Business case: Provide case counts by status and priority and a CSV export to support operational workload reporting.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>

<h1><bean:message key="ui.operational.reports"/></h1>

<p><bean:message key="ui.basic.legacy.counts.advanced.interactive.analytics.are.reserved.for.modernization"/></p>

<div class="row g-4 mb-4">
    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.by.status"/></h2>

            <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.by.status'/>">
                <table class="table table-striped table-hover align-middle">
                    <caption class="visually-hidden"><bean:message key="ui.by.status"/></caption>
                    <thead><tr>
                        <th scope="col"><bean:message key="ui.status"/></th>
                        <th scope="col"><bean:message key="ui.total"/></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="r" items="${reportForm.summary.statuses}">
                        <tr>
                            <td><c:out value="${r.label}"/></td>
                            <td><c:out value="${r.total}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </section>
    </div>

    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.by.priority"/></h2>
            <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.by.priority'/>">
                <table class="table table-striped table-hover align-middle">
                    <caption class="visually-hidden"><bean:message key="ui.by.priority"/></caption>
                    <thead><tr>
                        <th scope="col"><bean:message key="ui.priority"/></th>
                        <th scope="col"><bean:message key="ui.total"/></th>
                    </tr></thead>
                    <tbody>
                    <c:forEach var="r" items="${reportForm.summary.priorities}">
                        <tr>
                            <td><c:out value="${r.label}"/></td>
                            <td><c:out value="${r.total}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>

            <p><bean:message key="ui.overdue"/> <strong><c:out value="${reportForm.summary.overdue}"/></strong></p>
        </section>
    </div>
</div>

<a class="button btn btn-primary" href="${pageContext.request.contextPath}${viewOptions.routes.REPORTS_EXPORT}">
    <bean:message key="ui.export.csv.up.to.5.000.rows"/>
</a>
