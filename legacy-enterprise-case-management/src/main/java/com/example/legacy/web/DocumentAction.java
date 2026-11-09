package com.example.legacy.web;

import com.example.legacy.service.DocumentService;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.DocumentPageDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.StringsUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.FormFile;

/**
 * Handles multipart and download HTTP transport using document form DTOs.
 *
 * @author Chuck Emmons
 */
public final class DocumentAction extends Action {
  private final DocumentService documentService;

  /** Creates this action and its reusable service collaborators. */
  public DocumentAction() {
    this(new DocumentService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param documentService the document service collaborator
   * @throws NullPointerException if {@code documentService} is {@code null}
   */
  public DocumentAction(DocumentService documentService) {
    this.documentService = java.util.Objects.requireNonNull(documentService, "documentService");
  }

  /**
   * Converts the upload/download form to a DTO and delegates business validation and persistence to
   * services.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect, or {@code null} when the response is written
   *     directly
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionForward execute(
      ActionMapping mapping,
      ActionForm form,
      HttpServletRequest request,
      HttpServletResponse response) {
    UploadForm uploadForm = (UploadForm) form;
    FormFile file = uploadForm.getFile();
    try {
      String actor = WebUtil.getAuthenticatedUsername(request);
      if (ActionOperation.UPLOAD.matchesParameter(mapping.getParameter())) {
        WebUtil.validateCsrfToken(
            uploadForm.getCsrf(), request.getSession().getAttribute(WebKeys.CSRF));
        UploadDocumentDto data = uploadForm.getUploadData();
        documentService.uploadDocument(actor, data);
        return WebUtil.createRedirect(
            request,
            Routes.CASE_VIEW + "?id=" + data.getCaseId(),
            MessageKeys.CONFIRMATION_DOCUMENT_UPLOADED);
      }
      uploadForm.setData(
          DocumentPageDto.builder()
              .document(documentService.downloadDocument(actor, uploadForm.getRecordData()))
              .build());
      DocumentDto document = uploadForm.getDocument();
      response.setContentType(ContentTypes.BINARY);
      response.setContentLength(document.getContentSize());
      response.setHeader(
          HttpConstants.CONTENT_DISPOSITION,
          "attachment; filename=\""
              + StringsUtil.sanitizeFilename(document.getOriginalName())
              + "\"");
      response.setHeader(HttpConstants.CONTENT_TYPE_OPTIONS, HttpConstants.NO_SNIFF);
      response.getOutputStream().write(document.getPayload());
      return null;
    } catch (BusinessValidationException | ConcurrentUpdateException exception) {
      WebUtil.addBusinessError(request, exception);
      return new ActionForward(mapping.getInput());
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    } finally {
      if (file != null) {
        file.destroy();
      }
    }
  }
}
