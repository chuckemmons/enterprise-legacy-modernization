package com.example.legacy.web;

import com.example.legacy.service.AuditService;
import com.example.legacy.service.dto.AuditPageDto;
import com.example.legacy.service.dto.PageRequestDto;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Presents audit DTOs through a request-scoped Struts form.
 *
 * @author Chuck Emmons
 */
public final class AuditAction extends Action {
  private final AuditService auditService;

  /** Creates the audit action and its reusable service collaborator. */
  public AuditAction() {
    this(new AuditService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param auditService the audit service collaborator
   * @throws NullPointerException if {@code auditService} is {@code null}
   */
  public AuditAction(AuditService auditService) {
    this.auditService = java.util.Objects.requireNonNull(auditService, "auditService");
  }

  /**
   * Maps bound pagination to service input and sets mapped audit results on the form.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionForward execute(
      ActionMapping mapping,
      ActionForm form,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      AuditForm auditForm = (AuditForm) form;
      auditForm.setData(
          AuditPageDto.builder(auditForm.getData())
              .events(
                  auditService.listRecentAuditEvents(
                      WebUtil.getAuthenticatedUsername(request),
                      PageRequestDto.builder()
                          .page(FormDataUtil.parsePageIndex(auditForm.getPageNumber()))
                          .build()))
              .build());
      return WebUtil.createViewForward(PageView.AUDIT);
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    }
  }
}
