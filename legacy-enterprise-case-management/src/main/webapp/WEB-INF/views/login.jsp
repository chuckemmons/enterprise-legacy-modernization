<%--
    Business case: Authenticate provisioned users before they access case records and role-controlled business operations.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>

<section class="panel card card-body narrow">
    <h1><bean:message key="ui.sign.in"/></h1>

    <p class="text-danger"><bean:message key="ui.required.fields"/></p>
    <p><bean:message key="ui.use.your.provisioned.user.account.demo.seed.users.are.optional"/></p>

    <form action="${pageContext.request.contextPath}${viewOptions.routes.AUTHENTICATE}" method="post">
        <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">

        <label class="form-label d-block mb-3" for="login-username">
            <fmt:message key="ui.username"/> <span class="text-danger" aria-hidden="true">*</span>
            <input aria-describedby="login-username-error" class="form-control" id="login-username" name="username"
                    value="<c:out value='${authForm.username}'/>" required maxlength="${viewOptions.limits.USERNAME}"
                    autocomplete="username">
        </label>
        <div id="login-username-error" class="field-errors">
            <html:messages id="usernameError" property="username">
                <p class="invalid-feedback d-block"><c:out value="${usernameError}"/></p>
            </html:messages>
        </div>

        <label class="form-label d-block mb-3" for="login-password">
            <fmt:message key="ui.password"/> <span class="text-danger" aria-hidden="true">*</span>
            <input aria-describedby="login-password-error" class="form-control" id="login-password" type="password"
                    name="password" required autocomplete="current-password">
        </label>
        <div id="login-password-error" class="field-errors">
            <html:messages id="passwordError" property="password">
                <p class="invalid-feedback d-block"><c:out value="${passwordError}"/></p>
            </html:messages>
        </div>

        <button class="btn btn-primary w-100" type="submit"><bean:message key="ui.sign.in"/></button>
    </form>
</section>
