<%--
    Business case: Present validation errors, request failures, and success confirmations
    so users know the outcome of their actions.

    Author: Chuck Emmons
--%>
<%@ include file="taglibs.jspf" %>
<html:errors/>
<c:if test="${not empty param[viewOptions.keys.MESSAGE]}">
    <div class="success alert alert-success" role="status"><c:out value="${param[viewOptions.keys.MESSAGE]}"/></div>
</c:if>
<c:if test="${not empty requestScope[viewOptions.keys.ERROR]}">
    <div class="alert alert-danger" role="alert" tabindex="-1" data-error-summary>
        <c:out value="${requestScope[viewOptions.keys.ERROR]}"/>
    </div>
</c:if>
