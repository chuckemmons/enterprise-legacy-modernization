<%--
    Business case: Provide shared navigation to business functions appropriate to
    the signed-in user role and a sign-out action.

    Author: Chuck Emmons
--%>
<%@ include file="taglibs.jspf" %>
<header class="navbar navbar-expand-xl bg-dark navbar-dark" data-bs-theme="dark">
    <div class="container">
        <span class="navbar-brand text-wrap"><bean:message key="ui.legacy.enterprise.case.management"/></span>
        <c:if test="${not empty sessionScope[viewOptions.keys.ACTOR]}">
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#primary-navigation"
                    aria-controls="primary-navigation" aria-expanded="false" aria-label="<bean:message key='ui.toggle.navigation'/>">
                <span class="navbar-toggler-icon"></span>
            </button>
            <nav id="primary-navigation" class="collapse navbar-collapse" aria-label="<bean:message key='ui.primary.navigation'/>">
                <div class="navbar-nav ms-auto gap-xl-2">
                    <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.DASHBOARD}">
                        <bean:message key="ui.dashboard"/>
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.CASES}">
                        <bean:message key="ui.cases"/>
                    </a>
                    <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.NOTIFICATIONS}">
                        <bean:message key="ui.notifications"/>
                    </a>
                    <c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN
                            or sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.SUPERVISOR
                            or sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.AUDITOR}">
                        <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.REPORTS}">
                            <bean:message key="ui.reports"/>
                        </a>
                        <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.AUDIT}">
                            <bean:message key="ui.audit"/>
                        </a>
                    </c:if>
                    <c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN}">
                        <a class="nav-link" href="${pageContext.request.contextPath}${viewOptions.routes.USERS}">
                            <bean:message key="ui.users"/>
                        </a>
                    </c:if>
                </div>
                <form class="ms-xl-3 mt-3 mt-xl-0" action="${pageContext.request.contextPath}${viewOptions.routes.LOGOUT}" method="post">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <button type="submit" class="btn btn-outline-light"><bean:message key="ui.sign.out"/></button>
                </form>
            </nav>
        </c:if>
    </div>
</header>
