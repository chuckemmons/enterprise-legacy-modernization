<%--
    Business case: Provide the common page layout, navigation, feedback, and
    accessibility structure for a consistent case-management experience.

    Author: Chuck Emmons
--%>
<%@ include file="../fragments/taglibs.jspf" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<!DOCTYPE html>
<html lang="en">

<head>
    <script>document.documentElement.classList.add("js");</script>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <tiles:importAttribute name="titleKey"/>
    <title><bean:message key="${titleKey}"/> | <bean:message key="application.shortName"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/bootstrap-5.3.8.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/site.css">
</head>

<body class="d-flex flex-column min-vh-100">
    <a class="visually-hidden-focusable skip-link" href="#main-content"><bean:message key="ui.skip.to.content"/></a>

    <tiles:insert attribute="header" flush="false"/>

    <main id="main-content" class="container flex-grow-1 py-4" tabindex="-1">
        <tiles:insert attribute="messages" flush="false"/>
        <tiles:insert attribute="body" flush="false"/>
    </main>

    <tiles:insert attribute="footer" flush="false"/>

    <script src="${pageContext.request.contextPath}/assets/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/vendor/bootstrap-5.3.8.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/site.js"></script>
</body>
</html>
