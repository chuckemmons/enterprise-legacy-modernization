package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.AuditService;
import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.CaseService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.support.ServletTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.ActionOperation;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.PageView;
import com.example.legacy.web.ReportForm;
import com.example.legacy.web.UserForm;
import com.example.legacy.web.ValidationInputAction;
import com.example.legacy.web.WebKeys;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Verifies read-only input redisplay preserves submitted controls, validation errors, and secret
 * cleanup.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ValidationInputActionTest {
  @Mock private CaseService caseService;
  @Mock private BackOfficeService backOfficeService;
  @Mock private AuditService auditService;
  @Mock private NotificationService notificationService;
  private ValidationInputAction action;
  private HttpServletRequest request;

  /**
   * Creates an authenticated request and isolated service collaborators without database access.
   */
  @Before
  public void setUp() {
    request = ServletTestSupport.createRequest(null, Collections.emptyMap());
    action =
        new ValidationInputAction(
            caseService, backOfficeService, auditService, notificationService);
  }

  /**
   * Preserves rejected edit values and the submitted version while refreshing server-owned case
   * data.
   *
   * @throws AssertionError if redisplay replaces submitted controls or looks up the wrong case
   * @throws RuntimeException if configuring the case lookup or presenting request errors fails
   */
  @Test
  public void preservesRejectedUpdateControls() {
    final CaseDto persisted = stubCaseDetails();
    CaseForm form = new CaseForm();
    form.setId("7");
    form.setTitle("");
    form.setDue("invalid-date");
    form.setVersion("submitted-version");
    request.setAttribute(WebKeys.VALIDATION_OPERATION, ActionOperation.UPDATE.getParameter());
    ActionForward result = executeInput(ActionOperation.CASE, form);
    assertEquals(PageView.CASE_VIEW.getDefinition(), result.getPath());
    assertEquals("", form.getTitle());
    assertEquals("invalid-date", form.getDue());
    assertEquals("submitted-version", form.getVersion());
    assertSame(persisted, form.getItem());
    ArgumentCaptor<RecordReferenceDto> query = ArgumentCaptor.forClass(RecordReferenceDto.class);
    verify(caseService).getCaseDetails(eq("admin"), query.capture());
    assertEquals(7L, query.getValue().getId());
    verifyNoMoreInteractions(caseService);
  }

  /**
   * Retains assignment, transition, and comment controls while refreshing the persisted case
   * fields.
   *
   * @throws AssertionError if auxiliary command fields are lost during redisplay
   * @throws RuntimeException if configuring the case lookup or presenting request errors fails
   */
  @Test
  public void preservesAuxiliaryCommandControls() {
    stubCaseDetails();
    CaseForm form = new CaseForm();
    form.setId("7");
    form.setAssignee("worker");
    form.setDestination("APPROVED");
    form.setBody("Submitted comment");
    request.setAttribute(WebKeys.VALIDATION_OPERATION, ActionOperation.COMMENT.getParameter());
    assertEquals(
        PageView.CASE_VIEW.getDefinition(), executeInput(ActionOperation.CASE, form).getPath());
    assertEquals("Database title", form.getTitle());
    assertEquals("9", form.getVersion());
    assertEquals("worker", form.getAssignee());
    assertEquals("APPROVED", form.getDestination());
    assertEquals("Submitted comment", form.getBody());
  }

  /**
   * Selects each summary service and its matching input page without mixing report and dashboard
   * data.
   *
   * @throws AssertionError if either route loads or displays the wrong summary
   * @throws RuntimeException if configuring summary services or presenting request errors fails
   */
  @Test
  public void loadsReportAndDashboardSummariesForTheirOwnPages() {
    CaseSummary report = CaseSummary.builder().overdue(3L).build();
    CaseSummary dashboard = CaseSummary.builder().overdue(8L).build();
    when(backOfficeService.getReportSummary("admin")).thenReturn(report);
    when(backOfficeService.getDashboardSummary("admin")).thenReturn(dashboard);
    ReportForm reportForm = new ReportForm();
    ReportForm dashboardForm = new ReportForm();
    assertEquals(
        PageView.REPORTS.getDefinition(),
        executeInput(ActionOperation.REPORTS, reportForm).getPath());
    assertEquals(
        PageView.DASHBOARD.getDefinition(),
        executeInput(ActionOperation.DASHBOARD, dashboardForm).getPath());
    assertSame(report, reportForm.getSummary());
    assertSame(dashboard, dashboardForm.getSummary());
    verify(backOfficeService).getReportSummary("admin");
    verify(backOfficeService).getDashboardSummary("admin");
    verifyNoMoreInteractions(backOfficeService);
  }

  /**
   * Clears account-creation passwords after successfully reloading the user listing.
   *
   * @throws AssertionError if redisplay retains the password or replaces correctable user controls
   * @throws RuntimeException if configuring account lookup or presenting request errors fails
   */
  @Test
  public void clearsPasswordAfterSuccessfulUserRedisplay() {
    when(backOfficeService.listAllAccounts("admin")).thenReturn(Collections.emptyList());
    UserForm form = new UserForm();
    form.setUsername("SubmittedUser");
    form.setPassword("SubmittedSecret");
    assertEquals(
        PageView.USERS.getDefinition(), executeInput(ActionOperation.USERS, form).getPath());
    assertNull(form.getPassword());
    assertEquals("SubmittedUser", form.getUsername());
    verify(backOfficeService).listAllAccounts("admin");
    verifyNoMoreInteractions(backOfficeService);
  }

  /**
   * Keeps the original validation errors and clears the password even when display-data loading
   * fails.
   *
   * @throws AssertionError if error preservation or password cleanup changes on a failed reload
   * @throws RuntimeException if configuring the account lookup or presenting request errors fails
   */
  @Test
  public void preservesExistingErrorsAndClearsPasswordAfterReloadFailure() {
    when(backOfficeService.listAllAccounts("admin"))
        .thenThrow(new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID));
    ActionErrors errors = new ActionErrors();
    errors.add("username", new ActionMessage(MessageKeys.BUSINESS_FIELD_REQUIRED));
    request.setAttribute(Globals.ERROR_KEY, errors);
    UserForm form = new UserForm();
    form.setPassword("SubmittedSecret");
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.USERS, form).getPath());
    assertSame(errors, request.getAttribute(Globals.ERROR_KEY));
    assertNull(form.getPassword());
  }

  /**
   * Preserves the original errors when an invalid case identifier prevents rebuilding the detail
   * page.
   *
   * @throws AssertionError if invalid input reaches persistence or replaces the original errors
   * @throws RuntimeException if request-error presentation fails
   */
  @Test
  public void rejectsInvalidCaseReferenceWithoutLoadingData() {
    ActionErrors errors = new ActionErrors();
    errors.add("id", new ActionMessage(MessageKeys.BUSINESS_INVALID_RECORD_ID));
    request.setAttribute(Globals.ERROR_KEY, errors);
    CaseForm form = new CaseForm();
    form.setId("invalid");
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.CASE, form).getPath());
    assertSame(errors, request.getAttribute(Globals.ERROR_KEY));
    verifyNoInteractions(caseService, backOfficeService, auditService, notificationService);
  }

  /**
   * Reloads other read only input pages and rejects unknown pages.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reloadsOtherReadOnlyInputPagesAndRejectsUnknownPages() {
    when(caseService.searchCases(
            eq("admin"), any(com.example.legacy.service.dto.CaseSearchCriteriaDto.class)))
        .thenReturn(Collections.emptyList());
    assertEquals(
        PageView.CASES.getDefinition(),
        executeInput(ActionOperation.CASES, new CaseForm()).getPath());
    when(auditService.listRecentAuditEvents(
            eq("admin"), any(com.example.legacy.service.dto.PageRequestDto.class)))
        .thenReturn(Collections.emptyList());
    assertEquals(
        PageView.AUDIT.getDefinition(),
        executeInput(ActionOperation.AUDIT, new com.example.legacy.web.AuditForm()).getPath());
    when(notificationService.listNotifications("admin")).thenReturn(Collections.emptyList());
    assertEquals(
        PageView.NOTIFICATIONS.getDefinition(),
        executeInput(ActionOperation.NOTIFICATIONS, new com.example.legacy.web.NotificationForm())
            .getPath());
    assertEquals(
        PageView.ERROR.getDefinition(),
        executeInput(ActionOperation.CREATE, new CaseForm()).getPath());
  }

  /**
   * Presents reload failures when no original validation errors exist.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void presentsReloadFailuresWhenNoOriginalValidationErrorsExist() {
    CaseForm form = new CaseForm();
    form.setId("invalid");
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.CASE, form).getPath());
    assertTrue(request.getAttribute(Globals.ERROR_KEY) instanceof ActionErrors);
    request.setAttribute(Globals.ERROR_KEY, new ActionErrors());
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.CASE, form).getPath());
    when(backOfficeService.listAllAccounts("admin"))
        .thenThrow(new IllegalStateException("reload failed"));
    UserForm user = new UserForm();
    user.setPassword("secret");
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.USERS, user).getPath());
    assertNull(user.getPassword());
    CaseForm missing = new CaseForm();
    assertEquals(
        PageView.ERROR.getDefinition(), executeInput(ActionOperation.CASE, missing).getPath());
  }

  /**
   * Supplies persisted case details for checking which submitted controls survive redisplay.
   *
   * @return the persisted case snapshot
   * @throws RuntimeException if configuring the mocked case lookup fails
   */
  private CaseDto stubCaseDetails() {
    CaseDto persisted = CaseDto.builder().id(7L).title("Database title").version(9).build();
    when(caseService.getCaseDetails(eq("admin"), any(RecordReferenceDto.class)))
        .thenReturn(CaseDetails.builder().item(persisted).build());
    return persisted;
  }

  /**
   * Executes a configured read-only input route against the current request.
   *
   * @param operation the input-page operation to execute
   * @param form the form whose display state is refreshed
   * @return the selected page forward
   * @throws RuntimeException if request-error presentation fails
   * @throws NullPointerException if {@code operation} is {@code null}
   */
  private ActionForward executeInput(ActionOperation operation, ActionForm form) {
    ActionMapping mapping = new ActionMapping();
    mapping.setParameter(operation.getParameter());
    return action.execute(mapping, form, request, null);
  }
}
