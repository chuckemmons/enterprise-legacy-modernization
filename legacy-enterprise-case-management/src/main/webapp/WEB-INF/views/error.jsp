<%--
    Business case: Explain that a request failed and provide a route back to the dashboard so users can resume their work.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>
<bean:define id="errorFallback">
    <bean:message key="ui.the.request.could.not.be.completed.return.to.the.previous.page.and.try.again"/>
</bean:define>


<section class="panel card card-body">
    <h1><bean:message key="ui.request.could.not.be.completed"/></h1>

    <c:if test="${empty requestScope['org.apache.struts.action.ERROR']}">
        <p><c:out value="${requestScope[viewOptions.keys.ERROR]}" default="${errorFallback}"/></p>
    </c:if>

    <a href="${pageContext.request.contextPath}${viewOptions.routes.DASHBOARD}">
        <bean:message key="ui.return.to.dashboard"/>
    </a>
</section>
