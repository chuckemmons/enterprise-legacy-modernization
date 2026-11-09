package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.CaseService;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.MessageArgument;
import com.example.legacy.support.ServletTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.ActionOperation;
import com.example.legacy.web.CaseAction;
import com.example.legacy.web.CaseForm;
import com.example.legacy.web.PageView;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
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
 * Tests the HTTP/application boundary with a Mockito case-service substitute.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CaseActionTest {
  @Mock private CaseService service;
  private HttpServletRequest request;
  private CaseForm form;
  private ActionMapping mapping;

  /** Creates independent bound controls and request/session state for each test. */
  @Before
  public void setUp() {
    request = ServletTestSupport.createRequest(null, Collections.emptyMap());
    form = new CaseForm();
    form.setTitle("Bound title");
    form.setDescription("Description");
    form.setCategory("GENERAL");
    form.setPriority("MEDIUM");
    form.setDepartment("Operations");
    form.setDue("2026-11-20");
    mapping = new ActionMapping();
    mapping.setParameter(ActionOperation.CREATE.getParameter());
    mapping.setInput("page.case-new");
  }

  /**
   * Maps creation controls to the operation DTO and redirects using the returned identity.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void delegatesCreationAndMapsResponse() {
    when(service.createCase(eq("admin"), any(CreateCaseDto.class)))
        .thenReturn(CaseDto.builder().id(7L).title("Bound title").version(0).build());
    ActionForward result = new CaseAction(service).execute(mapping, form, request, null);
    ArgumentCaptor<CreateCaseDto> command = ArgumentCaptor.forClass(CreateCaseDto.class);
    verify(service).createCase(eq("admin"), command.capture());
    assertEquals("Bound title", command.getValue().getTitle());
    assertTrue(result.getRedirect());
    assertTrue(result.getPath().contains("/caseView.do?id=7"));
    assertTrue(result.getPath().contains("Case+created"));
    assertEquals("7", form.getId());
  }

  /**
   * Ignores unrelated invalid controls and defensively captures operation date inputs.
   *
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void mapsOnlyRelevantOperationControls() {
    form.setVersion("invalid-version-unrelated-to-create");
    CreateCaseDto command = form.getCreateData();
    command.getDueAt().setTime(0);
    assertNotEquals(0L, command.getDueAt().getTime());
    form.setDue("invalid-date-unrelated-to-search");
    assertEquals(0, form.getSearchData().getPage());
    verifyNoInteractions(service);
  }

  /**
   * Presents expected keyed corrections on the original input page.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void presentsBusinessValidationOnInput() {
    when(service.createCase(eq("admin"), any(CreateCaseDto.class)))
        .thenThrow(
            new BusinessValidationException(
                MessageKeys.BUSINESS_FIELD_REQUIRED, new MessageArgument(MessageKeys.FIELD_TITLE)));
    ActionForward result = new CaseAction(service).execute(mapping, form, request, null);
    assertEquals(mapping.getInput(), result.getPath());
    ActionMessage error =
        (ActionMessage) ((ActionErrors) request.getAttribute(Globals.ERROR_KEY)).get().next();
    assertEquals(MessageKeys.BUSINESS_FIELD_REQUIRED, error.getKey());
    assertEquals("Title", error.getValues()[0]);
  }

  /**
   * Treats programming argument failures as unexpected failures with a generic browser message.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void hidesUnexpectedExceptionDetails() {
    when(service.createCase(eq("admin"), any(CreateCaseDto.class)))
        .thenThrow(new IllegalArgumentException("Internal programming error"));
    ActionForward result = new CaseAction(service).execute(mapping, form, request, null);
    assertEquals(PageView.ERROR.getDefinition(), result.getPath());
    ActionMessage error =
        (ActionMessage) ((ActionErrors) request.getAttribute(Globals.ERROR_KEY)).get().next();
    assertEquals(MessageKeys.VALIDATION_UNEXPECTED, error.getKey());
  }

  /**
   * Loads list new and detail pages through the matching read use cases.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void loadsListNewAndDetailPagesThroughTheMatchingReadUseCases() {
    CaseAction action = new CaseAction(service);
    mapping.setParameter("list");
    when(service.searchCases(
            eq("admin"), any(com.example.legacy.service.dto.CaseSearchCriteriaDto.class)))
        .thenReturn(Collections.singletonList(CaseDto.builder().title("Found").build()));
    assertEquals(
        PageView.CASES.getDefinition(), action.execute(mapping, form, request, null).getPath());
    assertEquals("Found", form.getCases().get(0).getTitle());
    mapping.setParameter("new");
    assertEquals(
        PageView.CASE_NEW.getDefinition(), action.execute(mapping, form, request, null).getPath());
    form.setId("7");
    mapping.setParameter("view");
    when(service.getCaseDetails(
            eq("admin"), any(com.example.legacy.service.dto.RecordReferenceDto.class)))
        .thenReturn(
            com.example.legacy.service.dto.CaseDetails.builder()
                .item(CaseDto.builder().id(7L).title("Details").build())
                .build());
    assertEquals(
        PageView.CASE_VIEW.getDefinition(), action.execute(mapping, form, request, null).getPath());
    assertEquals("Details", form.getTitle());
  }

  /**
   * Delegates each mutation and rejects unknown operations.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void delegatesEachMutationAndRejectsUnknownOperations() {
    final CaseAction action = new CaseAction(service);
    form.setId("7");
    form.setVersion("3");
    form.setAssignee("worker");
    form.setDestination("TRIAGED");
    form.setBody("Comment");
    mapping.setParameter("update");
    when(service.updateCase(eq("admin"), any(com.example.legacy.service.dto.UpdateCaseDto.class)))
        .thenReturn(CaseDto.builder().id(7L).title("Updated").build());
    assertTrue(action.execute(mapping, form, request, null).getRedirect());
    form.setAssignee("worker");
    mapping.setParameter("assign");
    assertTrue(action.execute(mapping, form, request, null).getRedirect());
    verify(service)
        .assignCase(eq("admin"), any(com.example.legacy.service.dto.AssignCaseDto.class));
    form.setDestination("TRIAGED");
    mapping.setParameter("transition");
    assertTrue(action.execute(mapping, form, request, null).getRedirect());
    verify(service)
        .transitionCase(eq("admin"), any(com.example.legacy.service.dto.TransitionCaseDto.class));
    form.setBody("Comment");
    mapping.setParameter("comment");
    assertTrue(action.execute(mapping, form, request, null).getRedirect());
    verify(service)
        .addCaseComment(eq("admin"), any(com.example.legacy.service.dto.AddCaseCommentDto.class));
    mapping.setParameter("archive");
    assertTrue(
        action
            .execute(mapping, form, request, null)
            .getPath()
            .startsWith(com.example.legacy.web.Routes.CASES));
    verify(service)
        .archiveCase(eq("admin"), any(com.example.legacy.service.dto.RecordReferenceDto.class));
    mapping.setParameter("unknown");
    assertEquals(
        PageView.ERROR.getDefinition(), action.execute(mapping, form, request, null).getPath());
  }
}
