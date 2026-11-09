<%--
    Business case: Route servlet-container failures into the standard error page so users receive consistent
    recovery guidance.

    Author: Chuck Emmons
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%-- Servlet-container error dispatches bypass the Struts processor, so insert the definition here. --%>
<tiles:insert definition="page.error" flush="false"/>
