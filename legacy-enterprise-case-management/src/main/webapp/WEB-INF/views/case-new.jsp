<%--
    Business case: Let case staff register a new service request with the details needed to prioritize and route the work.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>
<section class="panel card card-body">
    <h1><bean:message key="ui.open.a.case"/></h1>
    <p class="text-danger"><bean:message key="ui.required.fields"/></p>
    <form action="${pageContext.request.contextPath}${viewOptions.routes.CASE_CREATE}" method="post">
        <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">

        <label class="form-label d-block mb-3" for="new-case-title">
            <fmt:message key="ui.title"/> <span class="text-danger" aria-hidden="true">*</span>
            <input aria-describedby="new-case-title-error" class="form-control" id="new-case-title" name="title"
                value="<c:out value='${caseForm.title}'/>" maxlength="${viewOptions.limits.TITLE}" required>
        </label>
        <div id="new-case-title-error" class="field-errors">
            <html:messages id="titleError" property="title">
                <p class="invalid-feedback d-block"><c:out value="${titleError}"/></p>
            </html:messages>
        </div>

        <label class="form-label d-block mb-3" for="new-case-description">
            <fmt:message key="ui.description"/> <span class="text-danger" aria-hidden="true">*</span>
            <textarea aria-describedby="new-case-description-error" class="form-control" id="new-case-description"
                        name="description" rows="6" maxlength="${viewOptions.limits.DESCRIPTION}" required><c:out value="${caseForm.description}"/></textarea>
        </label>
        <div id="new-case-description-error" class="field-errors">
            <html:messages id="descriptionError" property="description">
                <p class="invalid-feedback d-block"><c:out value="${descriptionError}"/></p>
            </html:messages>
        </div>

        <div class="row">
            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-case-category">
                    <fmt:message key="ui.category"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-case-category-error" class="form-control"
                              id="new-case-category" name="category" maxlength="${viewOptions.limits.CATEGORY}"
                              value="<c:out value='${caseForm.category}' default='${viewOptions.defaultCategory}'/>"
                              required>
                </label>
                <div id="new-case-category-error" class="field-errors">
                    <html:messages id="categoryError" property="category">
                        <p class="invalid-feedback d-block"><c:out value="${categoryError}"/></p>
                    </html:messages>
                </div>
            </div>

            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-case-priority">
                    <fmt:message key="ui.priority"/> <span class="text-danger" aria-hidden="true">*</span>
                    <select aria-describedby="new-case-priority-error" class="form-select" id="new-case-priority"
                            name="priority">
                        <c:forEach items="${viewOptions.priorities}" var="priority">
                            <option <c:if test="${priority eq (empty caseForm.priority
                                            ? viewOptions.defaultPriority : caseForm.priority)}">selected</c:if>>
                                <c:out value="${priority}"/>
                            </option>
                        </c:forEach>
                    </select>
                </label>
                <div id="new-case-priority-error" class="field-errors">
                    <html:messages id="priorityError" property="priority">
                        <p class="invalid-feedback d-block"><c:out value="${priorityError}"/></p>
                    </html:messages>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-case-department">
                    <fmt:message key="ui.department"/> <span class="text-danger" aria-hidden="true">*</span>
                    <input aria-describedby="new-case-department-error" class="form-control"
                            id="new-case-department" name="department"
                            maxlength="${viewOptions.limits.DEPARTMENT}"
                            value="<c:out value='${caseForm.department}' default='${viewOptions.defaultDepartment}'/>"
                            required>
                </label>
                <div id="new-case-department-error" class="field-errors">
                    <html:messages id="departmentError" property="department">
                        <p class="invalid-feedback d-block"><c:out value="${departmentError}"/></p>
                    </html:messages>
                </div>
            </div>

            <div class="col-12 col-md-6 mb-3">
                <label class="form-label d-block" for="new-case-due">
                    <fmt:message key="ui.due.date"/>
                    <input aria-describedby="new-case-due-error" class="form-control" id="new-case-due"
                                          type="date" name="due" value="<c:out value='${caseForm.due}'/>">
                </label>
                <div id="new-case-due-error" class="field-errors">
                    <html:messages id="dueError" property="due">
                        <p class="invalid-feedback d-block"><c:out value="${dueError}"/></p>
                    </html:messages>
                </div>
            </div>
        </div>

        <button class="btn btn-primary"><bean:message key="ui.create.case"/></button>

        <a href="${pageContext.request.contextPath}${viewOptions.routes.CASES}"><bean:message key="ui.cancel"/></a>
    </form>
</section>
