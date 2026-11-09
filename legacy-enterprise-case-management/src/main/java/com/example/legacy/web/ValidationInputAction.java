package com.example.legacy.web;

import com.example.legacy.service.AuditService;
import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.CaseService;
import com.example.legacy.service.DefaultCaseService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.AuditPageDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.dto.NotificationPageDto;
import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.ReportPageDto;
import com.example.legacy.service.dto.UserPageDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.Globals;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;

/**
 * Reloads read-only display data after Struts rejects a submitted form, without retrying mutations.
 *
 * @author Chuck Emmons
 */
public final class ValidationInputAction extends Action {
  private final CaseService caseService;
  private final BackOfficeService backOfficeService;
  private final AuditService auditService;
  private final NotificationService notificationService;

  /** Creates reusable service collaborators for validation-error presentation. */
  public ValidationInputAction() {
    this(
        new DefaultCaseService(),
        new BackOfficeService(),
        new AuditService(),
        new NotificationService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param caseService the case service collaborator
   * @param backOfficeService the back office service collaborator
   * @param auditService the audit service collaborator
   * @param notificationService the notification service collaborator
   * @throws NullPointerException if {@code caseService} or {@code backOfficeService} or {@code
   *     auditService} or {@code notificationService} is {@code null}
   */
  public ValidationInputAction(
      CaseService caseService,
      BackOfficeService backOfficeService,
      AuditService auditService,
      NotificationService notificationService) {
    this.caseService = java.util.Objects.requireNonNull(caseService, "caseService");
    this.backOfficeService =
        java.util.Objects.requireNonNull(backOfficeService, "backOfficeService");
    this.auditService = java.util.Objects.requireNonNull(auditService, "auditService");
    this.notificationService =
        java.util.Objects.requireNonNull(notificationService, "notificationService");
  }

  /**
   * Prepares the original input page while preserving request errors and correctable submitted
   * fields.
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
      String actor = WebUtil.getAuthenticatedUsername(request);
      return prepareInputPage(actor, mapping.getParameter(), form, request);
    } catch (BusinessValidationException | ConcurrentUpdateException exception) {
      // An invalid record ID cannot be used to rebuild a detail page; keep the original form
      // errors.
      Object errors = request.getAttribute(Globals.ERROR_KEY);
      if (errors instanceof ActionMessages && !((ActionMessages) errors).isEmpty()) {
        return WebUtil.createViewForward(PageView.ERROR);
      }
      return WebUtil.createFailureForward(request, exception);
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    } finally {
      if (form instanceof UserForm) {
        ((UserForm) form).setPassword(null);
      }
    }
  }

  /**
   * Reloads read-only data for the requested input page after validation rejects a submission.
   *
   * @param actor the authenticated actor's username
   * @param page the configured input-page operation
   * @param form the form whose display data is refreshed
   * @param request the request carrying the rejected operation and submitted controls
   * @return the refreshed input page, or the error page for an unsupported operation
   * @throws RuntimeException if record validation, authorization, or display-data loading fails
   */
  private ActionForward prepareInputPage(
      String actor, String page, ActionForm form, HttpServletRequest request) {
    if (ActionOperation.CASE.matchesParameter(page)) {
      return prepareCaseInput(actor, (CaseForm) form, request);
    }
    if (ActionOperation.CASES.matchesParameter(page)) {
      CaseForm caseForm = (CaseForm) form;
      caseForm.setDisplayData(
          CasePageDto.builder()
              .cases(caseService.searchCases(actor, CaseSearchCriteriaDto.builder().build()))
              .build());
      return WebUtil.createViewForward(PageView.CASES);
    }
    if (ActionOperation.USERS.matchesParameter(page)) {
      ((UserForm) form)
          .setDisplayData(
              UserPageDto.builder().users(backOfficeService.listAllAccounts(actor)).build());
      return WebUtil.createViewForward(PageView.USERS);
    }
    if (ActionOperation.AUDIT.matchesParameter(page)) {
      ((AuditForm) form)
          .setData(
              AuditPageDto.builder()
                  .events(
                      auditService.listRecentAuditEvents(actor, PageRequestDto.builder().build()))
                  .build());
      return WebUtil.createViewForward(PageView.AUDIT);
    }
    if (ActionOperation.NOTIFICATIONS.matchesParameter(page)) {
      ((NotificationForm) form)
          .setData(
              NotificationPageDto.builder()
                  .notices(notificationService.listNotifications(actor))
                  .build());
      return WebUtil.createViewForward(PageView.NOTIFICATIONS);
    }
    if (ActionOperation.REPORTS.matchesParameter(page)
        || ActionOperation.DASHBOARD.matchesParameter(page)) {
      boolean reporting = ActionOperation.REPORTS.matchesParameter(page);
      ((ReportForm) form)
          .setData(
              ReportPageDto.builder()
                  .summary(
                      reporting
                          ? backOfficeService.getReportSummary(actor)
                          : backOfficeService.getDashboardSummary(actor))
                  .build());
      return WebUtil.createViewForward(reporting ? PageView.REPORTS : PageView.DASHBOARD);
    }
    return WebUtil.createViewForward(PageView.ERROR);
  }

  /**
   * Reloads case details while retaining the rejected edit controls or auxiliary command fields.
   *
   * @param actor the authenticated actor's username
   * @param caseForm the form containing the case reference and submitted controls
   * @param request the request identifying the rejected operation
   * @return the case detail page, or the error page when no record is selected
   * @throws BusinessValidationException if the submitted case identifier is invalid
   * @throws RuntimeException if authorization, persistence, or display-data preparation fails
   */
  private ActionForward prepareCaseInput(
      String actor, CaseForm caseForm, HttpServletRequest request) {
    RecordReferenceDto query =
        RecordReferenceDto.builder().id(FormDataUtil.parseRecordId(caseForm.getId())).build();
    if (query.getId() == 0) {
      return WebUtil.createViewForward(PageView.ERROR);
    }
    CaseDetails details = caseService.getCaseDetails(actor, query);
    CasePageDto loaded =
        CasePageDto.builder()
            .id(details.getItem().getId())
            .item(details.getItem())
            .details(details)
            .build();
    if (ActionOperation.UPDATE.matchesParameter(
        (String) request.getAttribute(WebKeys.VALIDATION_OPERATION))) {
      caseForm.setDisplayData(loaded);
    } else {
      String assignee = caseForm.getAssignee();
      String destination = caseForm.getDestination();
      String body = caseForm.getBody();
      caseForm.setData(loaded);
      caseForm.setAssignee(assignee);
      caseForm.setDestination(destination);
      caseForm.setBody(body);
    }
    return WebUtil.createViewForward(PageView.CASE_VIEW);
  }
}
