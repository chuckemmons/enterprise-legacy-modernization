<%--
    Business case: Let staff correct a rejected document upload and choose the attachment again
    while retaining the case reference.

    Author: Chuck Emmons
--%>
<%@ include file="fragments/taglibs.jspf" %>

<section class="panel card card-body">
    <h1><bean:message key="ui.upload.document"/></h1>

    <p class="text-danger"><bean:message key="ui.required.fields"/></p>

    <p><bean:message key="ui.correct.the.reported.issue.and.choose.the.document.again.if.the.case"/></p>

    <form method="post" action="${pageContext.request.contextPath}${viewOptions.routes.CASE_UPLOAD}"
          enctype="multipart/form-data">
        <input type="hidden" name="csrf" value="${sessionScope[viewOptions.keys.CSRF]}">
        <input type="hidden" name="id" value="<c:out value='${uploadForm.id}'/>">

        <label class="form-label d-block mb-3" for="upload-file">
            <fmt:message key="ui.pdf.text.jpeg.or.png.maximum.2.mb"/> <span class="text-danger" aria-hidden="true">*</span>
            <input aria-describedby="upload-file-error" class="form-control" id="upload-file" type="file" name="file"
                   required>
        </label>

        <div id="upload-file-error" class="field-errors">
            <html:messages id="fieldError" property="file">
                <p class="invalid-feedback d-block"><c:out value="${fieldError}"/></p>
            </html:messages>
        </div>

        <button class="btn btn-primary"><bean:message key="ui.upload.document"/></button>
    </form>

    <c:url var="caseUrl" value="${viewOptions.routes.CASE_VIEW}"><c:param name="id" value="${uploadForm.id}"/></c:url>

    <p>
        <a href="<c:out value='${caseUrl}'/>"><bean:message key="ui.return.to.case"/></a> ·
        <a href="${pageContext.request.contextPath}${viewOptions.routes.CASES}"><bean:message key="ui.case.list.link"/></a>
    </p>
</section>
