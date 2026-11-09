<%--
    Business case: Let administrators provision staff accounts, assign access roles,
    and enable or disable access to case-management functions.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>

<h1><bean:message key="ui.user.administration"/></h1>

<section class="panel card card-body">
    <h2><bean:message key="ui.create.account"/></h2>

    <p class="text-danger"><bean:message key="ui.required.fields"/></p>

    <form action="${pageContext.request.contextPath}${viewOptions.routes.USER_CREATE}" method="post">
        <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">

        <div class="row">
            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-username">
                    <fmt:message key="ui.username"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-user-username-error" class="form-control" id="new-user-username"
                           name="username" value="<c:out value='${userForm.username}'/>" required
                           maxlength="${viewOptions.limits.USERNAME}">
                </label>
                <div id="new-user-username-error" class="field-errors">
                    <html:messages id="usernameError" property="username">
                        <p class="invalid-feedback d-block"><c:out value="${usernameError}"/></p>
                    </html:messages>
                </div>
            </div>

            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-password">
                    <fmt:message key="ui.initial.password.12.chars"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-user-password-error" class="form-control" id="new-user-password"
                           autocomplete="new-password" type="password" name="password"
                           minlength="${viewOptions.limits.PASSWORD_MIN}" required>
                </label>
                <div id="new-user-password-error" class="field-errors">
                    <html:messages id="passwordError" property="password">
                        <p class="invalid-feedback d-block"><c:out value="${passwordError}"/></p>
                    </html:messages>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-name">
                    <fmt:message key="ui.name"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-user-name-error" class="form-control" id="new-user-name" name="name"
                           value="<c:out value='${userForm.name}'/>" required maxlength="${viewOptions.limits.DISPLAY_NAME}">
                </label>
                <div id="new-user-name-error" class="field-errors">
                    <html:messages id="nameError" property="name">
                        <p class="invalid-feedback d-block"><c:out value="${nameError}"/></p>
                    </html:messages>
                </div>
            </div>

            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-email">
                    <fmt:message key="ui.email"/>
                    <input aria-describedby="new-user-email-error" class="form-control" id="new-user-email" type="email"
                           name="email" value="<c:out value='${userForm.email}'/>" maxlength="${viewOptions.limits.EMAIL}">
                </label>
                <div id="new-user-email-error" class="field-errors">
                    <html:messages id="emailError" property="email">
                        <p class="invalid-feedback d-block"><c:out value="${emailError}"/></p>
                    </html:messages>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-department">
                    <fmt:message key="ui.department"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-user-department-error" class="form-control" id="new-user-department"
                           name="department" value="<c:out value='${userForm.department}'
                           default='${viewOptions.defaultDepartment}'/>" required maxlength="${viewOptions.limits.DEPARTMENT}">
                </label>
                <div id="new-user-department-error" class="field-errors">
                    <html:messages id="departmentError" property="department">
                        <p class="invalid-feedback d-block"><c:out value="${departmentError}"/></p>
                    </html:messages>
                </div>
            </div>

            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-user-role">
                    <fmt:message key="ui.role"/> <span class="text-danger" aria-hidden="true">*</span>
                    <select aria-describedby="new-user-role-error" class="form-select" id="new-user-role" name="role">
                        <c:forEach var="role" items="${viewOptions.roles}">
                            <option <c:if test="${role eq (empty userForm.role ? viewOptions.roleCodes.WORKER : userForm.role)}">selected</c:if>>
                                <c:out value="${role}"/>
                            </option>
                        </c:forEach>
                    </select>
                </label>
                <div id="new-user-role-error" class="field-errors">
                    <html:messages id="roleError" property="role">
                        <p class="invalid-feedback d-block"><c:out value="${roleError}"/></p>
                    </html:messages>
                </div>
            </div>
        </div>

        <button class="btn btn-primary"><bean:message key="ui.create.user"/></button>
    </form>
</section>

<section class="panel card card-body">
    <h2><bean:message key="ui.accounts"/></h2>

    <c:set var="hasRequiredAccountFields" value="${false}"/>
    <c:forEach var="account" items="${userForm.users}">
        <c:if test="${account.username ne sessionScope[viewOptions.keys.ACTOR]}">
            <c:set var="hasRequiredAccountFields" value="${true}"/>
        </c:if>
    </c:forEach>

    <c:if test="${hasRequiredAccountFields}">
        <p class="text-danger"><bean:message key="ui.required.fields"/></p>
    </c:if>

    <div class="table-container">
        <div class="table-responsive" role="region" tabindex="0" aria-label="<bean:message key='ui.accounts'/>">
            <table class="table table-striped table-hover align-middle">
                <caption class="visually-hidden"><bean:message key="ui.accounts"/></caption>
                <thead><tr>
                    <th scope="col"><bean:message key="ui.user"/></th>
                    <th scope="col"><bean:message key="ui.name"/></th>
                    <th scope="col"><bean:message key="ui.role"/></th>
                    <th scope="col"><bean:message key="ui.department"/></th>
                    <th scope="col"><bean:message key="ui.enabled"/></th>
                    <th scope="col"><bean:message key="ui.manage"/></th>
                </tr></thead>
                <tbody>
                <c:forEach var="u" items="${userForm.users}">
                    <tr>
                        <td><c:out value="${u.username}"/></td>
                        <td><c:out value="${u.displayName}"/></td>
                        <td><c:out value="${u.role}"/></td>
                        <td><c:out value="${u.department}"/></td>
                        <td><c:out value="${u.enabled}"/></td>
                        <td>
                            <c:if test="${u.username ne sessionScope[viewOptions.keys.ACTOR]}">
                                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.USER_ROLE}"
                                      class="inline d-inline-flex flex-wrap align-items-center gap-2">
                                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                                    <input type="hidden" name="id" value="${u.id}">

                                    <label class="form-label mb-0" for="account-role-${u.id}">
                                        <fmt:message key="ui.role"/>
                                        <span class="text-danger" aria-hidden="true">*</span>
                                        <span class="visually-hidden">(<c:out value="${u.username}"/>)</span>
                                    </label>
                                    <select class="form-select" id="account-role-${u.id}" name="role">
                                        <c:forEach var="v" items="${viewOptions.roles}">
                                            <option <c:if test="${v eq u.role}">selected</c:if>><c:out value="${v}"/></option>
                                        </c:forEach>
                                    </select>

                                    <button class="minor btn btn-outline-secondary btn-sm">
                                        <bean:message key="ui.role"/>
                                        <span class="visually-hidden">: <c:out value="${u.username}"/></span>
                                    </button>
                                </form>

                                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.USER_DISABLE}"
                                      class="inline d-inline-flex flex-wrap align-items-center gap-2">
                                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                                    <input type="hidden" name="id" value="${u.id}">

                                    <button class="minor btn btn-outline-secondary btn-sm">
                                        <bean:message key="ui.toggle.active"/>
                                        <span class="visually-hidden">: <c:out value="${u.username}"/></span>
                                    </button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</section>
