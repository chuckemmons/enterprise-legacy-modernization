<%--
    Business case: Let authorized staff review and maintain a case, manage its assignment and workflow,
    and retain notes and supporting documents.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>
<bean:define id="format_datetime"><bean:message key="format.datetime"/></bean:define>
<bean:define id="unassignedLabel"><bean:message key="ui.unassigned"/></bean:define>


<p><a href="${pageContext.request.contextPath}${viewOptions.routes.CASES}"><bean:message key="ui.case.list"/></a></p>

<div class="between d-flex flex-wrap align-items-sm-center justify-content-between gap-3">
    <h1><c:out value="${caseForm.item.caseNumber}"/> — <c:out value="${caseForm.item.title}"/></h1>
    <span class="pill badge rounded-pill text-bg-secondary"><c:out value="${caseForm.item.status}"/></span>
</div>

<p>
    <bean:message key="ui.created.by"/> <c:out value="${caseForm.item.createdBy}"/>
    <bean:message key="ui.department.metadata"/> <c:out value="${caseForm.item.department}"/>
    <bean:message key="ui.last.updated"/>
    <fmt:formatDate value="${caseForm.item.updatedAt}" pattern="${format_datetime}"/>
    <bean:message key="ui.version"/>
    <c:out value="${caseForm.item.version}"/>
</p>

<div class="row g-4 mb-4">
    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.details"/></h2>
            <c:if test="${caseForm.details.mayEdit}">
                <p class="text-danger"><bean:message key="ui.required.fields"/></p>
            </c:if>
            <c:choose>
                <c:when test="${caseForm.details.mayEdit}">
                    <form action="${pageContext.request.contextPath}${viewOptions.routes.CASE_UPDATE}" method="post">
                        <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                        <input type="hidden" name="id" value="${caseForm.item.id}">
                        <input type="hidden" name="version" value="<c:out value='${caseForm.version}'/>">

                        <label class="form-label d-block mb-3" for="case-title">
                            <fmt:message key="ui.title"/> <span class="text-danger" aria-hidden="true">*</span>
                            <input aria-describedby="case-title-error" class="form-control" id="case-title" name="title"
                                   required maxlength="${viewOptions.limits.TITLE}" value="<c:out value='${caseForm.title}'/>">
                        </label>
                        <div id="case-title-error" class="field-errors">
                            <html:messages id="fieldError" property="title">
                                <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                            </html:messages>
                        </div>

                        <label class="form-label d-block mb-3" for="case-description">
                            <fmt:message key="ui.description"/> <span class="text-danger" aria-hidden="true">*</span>
                            <textarea aria-describedby="case-description-error" class="form-control"
                                      id="case-description" name="description" rows="5" required
                                      maxlength="${viewOptions.limits.DESCRIPTION}">
                                <c:out value="${caseForm.description}"/>
                            </textarea>
                        </label>
                        <div id="case-description-error" class="field-errors">
                            <html:messages id="fieldError" property="description">
                                <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                            </html:messages>
                        </div>

                        <label class="form-label d-block mb-3" for="case-priority">
                            <fmt:message key="ui.priority"/> <span class="text-danger" aria-hidden="true">*</span>
                            <select aria-describedby="case-priority-error" class="form-select" id="case-priority"
                                    name="priority">
                                <c:forEach items="${viewOptions.priorities}" var="v">
                                    <option <c:if test="${caseForm.priority eq v}">selected</c:if>><c:out value="${v}"/></option>
                                </c:forEach>
                            </select>
                        </label>
                        <div id="case-priority-error" class="field-errors">
                            <html:messages id="fieldError" property="priority">
                                <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                            </html:messages>
                        </div>

                        <label class="form-label d-block mb-3" for="case-due">
                            <fmt:message key="ui.due.date"/>
                            <input aria-describedby="case-due-error" class="form-control" id="case-due" type="date"
                                   name="due" value="<c:out value='${caseForm.due}'/>">
                        </label>
                        <div id="case-due-error" class="field-errors">
                            <html:messages id="fieldError" property="due">
                                <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                            </html:messages>
                        </div>

                        <button class="btn btn-primary"><bean:message key="ui.save.changes"/></button>
                    </form>
                </c:when>
                <c:otherwise>
                    <p><c:out value="${caseForm.item.description}"/></p>
                    <p><bean:message key="ui.priority.caption"/> <c:out value="${caseForm.item.priority}"/></p>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.workflow"/></h2>

            <c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN
                    or sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.SUPERVISOR
                    or not empty caseForm.details.nextStates}">
                <p class="text-danger"><bean:message key="ui.required.fields"/></p>
            </c:if>

            <p>
                <bean:message key="ui.assigned.to"/>
                <strong><c:out value="${caseForm.item.assignedTo}" default="${unassignedLabel}"/></strong>
            </p>

            <c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN
                    or sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.SUPERVISOR}">
                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_ASSIGN}">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${caseForm.item.id}">

                    <label class="form-label d-block mb-3" for="case-assignee">
                        <fmt:message key="ui.assign.to"/> <span class="text-danger" aria-hidden="true">*</span>
                        <select aria-describedby="case-assignee-error" class="form-select" id="case-assignee"
                                name="assignee">
                            <c:forEach var="u" items="${caseForm.details.users}">
                                <c:if test="${u.enabled and u.role ne viewOptions.roleCodes.AUDITOR}">
                                    <option value="<c:out value='${u.username}'/>"
                                            <c:if test="${u.username eq caseForm.assignee}">selected</c:if>>
                                        <c:out value="${u.displayName}"/> (<c:out value="${u.username}"/>)
                                    </option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </label>
                    <div id="case-assignee-error" class="field-errors">
                        <html:messages id="fieldError" property="assignee">
                            <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                        </html:messages>
                    </div>

                    <button class="btn btn-primary"><bean:message key="ui.assign"/></button>
                </form>
            </c:if>

            <c:if test="${not empty caseForm.details.nextStates}">
                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_TRANSITION}">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${caseForm.item.id}">

                    <label class="form-label d-block mb-3" for="case-destination">
                        <fmt:message key="ui.next.status"/> <span class="text-danger" aria-hidden="true">*</span>
                        <select aria-describedby="case-destination-error" class="form-select" id="case-destination"
                                name="destination">
                            <c:forEach items="${caseForm.details.nextStates}" var="next">
                                <option value="${next}" <c:if test="${next eq caseForm.destination}">selected</c:if>>
                                    <c:out value="${next}"/>
                                </option>
                            </c:forEach>
                        </select>
                    </label>
                    <div id="case-destination-error" class="field-errors">
                        <html:messages id="fieldError" property="destination">
                            <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                        </html:messages>
                    </div>

                    <button class="btn btn-primary"><bean:message key="ui.advance.workflow"/></button>
                </form>
            </c:if>

            <c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN}">
                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_ARCHIVE}"
                      id="archive-case-form" onsubmit="return window.confirm('Archive this case?');">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${caseForm.item.id}">

                    <button class="danger btn btn-danger"><bean:message key="ui.archive.case"/></button>
                </form>
            </c:if>
        </section>
    </div>
</div>

<div class="row g-4 mb-4">
    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.discussion.notes"/></h2>

            <c:if test="${caseForm.details.mayComment}">
                <p class="text-danger"><bean:message key="ui.required.fields"/></p>

                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_COMMENT}">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${caseForm.item.id}">

                    <label class="form-label d-block mb-3" for="case-body">
                        <fmt:message key="ui.new.note"/> <span class="text-danger" aria-hidden="true">*</span>
                        <textarea aria-describedby="case-body-error" class="form-control" id="case-body" name="body"
                                  rows="3" maxlength="${viewOptions.limits.COMMENT}" required>
                            <c:out value="${caseForm.body}"/>
                        </textarea>
                    </label>
                    <div id="case-body-error" class="field-errors">
                        <html:messages id="fieldError" property="body">
                            <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                        </html:messages>
                    </div>

                    <button class="btn btn-primary"><bean:message key="ui.add.note"/></button>
                </form>
            </c:if>

            <c:forEach var="n" items="${caseForm.details.notes}">
                <div class="entry">
                    <small>
                        <c:out value="${n.author}"/>
                        · <fmt:formatDate value="${n.createdAt}" pattern="${format_datetime}"/>
                    </small>
                    <p><c:out value="${n.body}"/></p>
                </div>
            </c:forEach>
        </section>
    </div>

    <div class="col-12 col-lg-6">
        <section class="panel card card-body h-100">
            <h2><bean:message key="ui.documents.max.2.mb"/></h2>

            <c:if test="${caseForm.details.mayEdit}">
                <p class="text-danger"><bean:message key="ui.required.fields"/></p>

                <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_UPLOAD}"
                      enctype="multipart/form-data">
                    <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
                    <input type="hidden" name="id" value="${caseForm.item.id}">

                    <label class="form-label d-block mb-3" for="case-file">
                        <fmt:message key="ui.attach.pdf.text.jpeg.or.png"/> <span class="text-danger" aria-hidden="true">*</span>
                        <input aria-describedby="case-file-error" class="form-control" id="case-file" type="file"
                                name="file" required>
                    </label>
                    <div id="case-file-error" class="field-errors">
                        <html:messages id="fieldError" property="file">
                            <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
                        </html:messages>
                    </div>

                    <button class="btn btn-primary"><bean:message key="ui.upload.document"/></button>
                </form>
            </c:if>

            <ul>
                <c:forEach var="doc" items="${caseForm.details.documents}">
                    <li>
                        <a href="${pageContext.request.contextPath}${viewOptions.routes.CASE_DOWNLOAD}?id=${doc.id}">
                            <c:out value="${doc.originalName}"/>
                        </a>
                        (<c:out value="${doc.contentSize}"/> <bean:message key="ui.bytes"/>
                    </li>
                </c:forEach>
            </ul>
        </section>
    </div>
</div>

<c:if test="${sessionScope[viewOptions.keys.ROLE] eq viewOptions.roleCodes.ADMIN}">
    <div class="modal fade" id="archive-confirmation" tabindex="-1" aria-labelledby="archive-title"
         aria-describedby="archive-description" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h2 class="modal-title fs-5" id="archive-title"><bean:message key="ui.archive.case"/></h2>

                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<bean:message key='ui.close'/>"></button>
                </div>
                <div class="modal-body" id="archive-description"><bean:message key="ui.archive.confirmation"/></div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" id="archive-cancel">
                        <bean:message key="ui.cancel"/>
                    </button>
                    <button type="button" class="btn btn-danger" id="archive-confirm">
                        <bean:message key="ui.archive.case"/>
                    </button>
                </div>
            </div>
        </div>
    </div>
</c:if>
