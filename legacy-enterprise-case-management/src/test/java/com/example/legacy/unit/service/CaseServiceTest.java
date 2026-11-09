package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.dao.CaseNoteDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.model.Account;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuditService;
import com.example.legacy.service.CasePolicyService;
import com.example.legacy.service.CaseService;
import com.example.legacy.service.DefaultCaseService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.service.exception.RecordNotFoundException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.CaseNoteMapper;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests case business orchestration independently of SQL, using Mockito DAO/access/audit
 * collaborators.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CaseServiceTest {
  @Mock private SessionFactory factory;
  @Mock private Session session;
  @Mock private Transaction transaction;
  @Mock private CaseDao cases;
  @Mock private CaseNoteDao notes;
  @Mock private CaseDocumentDao documents;
  @Mock private AccountAccessService access;
  @Mock private AuditService audit;
  @Mock private NotificationService notifications;
  private CaseService service;
  private Account admin;

  /** Uses real field mappers and policy with isolated mocked infrastructure collaborators. */
  @Before
  public void setUp() {
    service =
        new DefaultCaseService(
            new HibernateTransactionTemplate(factory),
            cases,
            notes,
            documents,
            new CaseMapper(),
            new CaseNoteMapper(),
            new DocumentMapper(),
            new AccountMapper(),
            access,
            new CasePolicyService(),
            audit,
            notifications);
    admin = new Account();
    admin.setUsername("admin");
    admin.setRole("ADMIN");
    admin.setEnabled(true);
  }

  /**
   * Writes a new case and audit using exactly the supplied session before committing.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
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
  public void createsCaseAndAuditAtomically() {
    stubAuthenticatedTransaction();
    stubGeneratedCaseId();
    CreateCaseDto request = createCaseRequest();
    CaseDto result = service.createCase("admin", request);
    assertEquals("CASE-000007", result.getCaseNumber());
    assertEquals("Case title", request.getTitle());
    ArgumentCaptor<ServiceCase> saved = ArgumentCaptor.forClass(ServiceCase.class);
    verify(cases).save(eq(session), saved.capture());
    assertEquals("NEW", saved.getValue().getStatus());
    assertEquals("admin", saved.getValue().getCreatedBy());
    InOrder order = inOrder(cases, audit, transaction, session);
    order.verify(cases).save(eq(session), any(ServiceCase.class));
    order.verify(cases).flush(session);
    order
        .verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.CASE),
            eq(7L),
            eq(AuditAction.CREATE),
            eq("admin"),
            contains("CASE-000007"));
    order.verify(cases).flush(session);
    order.verify(transaction).commit();
    order.verify(session).close();
    verifyNoInteractions(notifications);
  }

  /**
   * Rolls back rather than committing case data alone when its audit collaborator fails.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rollsBackWhenAuditFails() {
    stubAuthenticatedTransaction();
    stubGeneratedCaseId();
    RuntimeException expected = new IllegalStateException("audit failed");
    doThrow(expected)
        .when(audit)
        .recordAuditEvent(
            eq(session),
            any(AuditEntity.class),
            anyLong(),
            any(AuditAction.class),
            anyString(),
            anyString());
    assertSame(
        expected,
        assertThrows(
            RuntimeException.class, () -> service.createCase("admin", createCaseRequest())));
    verify(transaction).rollback();
    verify(transaction, never()).commit();
    verify(session).close();
  }

  /**
   * Uses the enclosing assignment transaction for its audit and recipient notification.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
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
  public void assignsAndNotifiesOnTheSameSession() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setCaseNumber("CASE-000007");
    item.setTitle("Case");
    item.setStatus("NEW");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    Account worker = new Account();
    worker.setUsername("worker");
    doReturn(worker).when(access).requireActiveAccount(session, "worker");
    service.assignCase("admin", AssignCaseDto.builder().id(7).assignee("worker").build());
    assertEquals("worker", item.getAssignedTo());
    verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.CASE),
            eq(7L),
            eq(AuditAction.ASSIGN),
            eq("admin"),
            anyString());
    verify(notifications)
        .createNotification(eq(session), eq("worker"), eq(7L), contains("CASE-000007"), eq("Case"));
    verify(transaction).commit();
    verify(session).close();
  }

  /**
   * Rejects a submitted stale version before writing audit or entity updates.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rejectsStaleSubmittedVersions() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setStatus("NEW");
    item.setVersion(3);
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    UpdateCaseDto request =
        UpdateCaseDto.builder()
            .id(7)
            .version(2)
            .title("Updated title")
            .description("Updated description")
            .priority("MEDIUM")
            .build();
    assertThrows(ConcurrentUpdateException.class, () -> service.updateCase("admin", request));
    verifyNoInteractions(audit, notifications);
    verify(cases, never()).flush(session);
    verify(transaction).rollback();
    verify(session).close();
  }

  /**
   * Returns an explicit not-found failure for unavailable records.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void reportsMissingCases() {
    stubAuthenticatedTransaction();
    assertThrows(
        RecordNotFoundException.class,
        () -> service.getCaseDetails("admin", RecordReferenceDto.builder().id(7).build()));
    verify(transaction).rollback();
    verify(session).close();
    verifyNoInteractions(audit, notifications);
  }

  /**
   * Searches normalized filters and maps matching cases.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void searchesNormalizedFiltersAndMapsMatchingCases() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setTitle("Found");
    when(cases.searchCases(session, "request", "NEW", "worker", 2, 25))
        .thenReturn(java.util.Collections.singletonList(item));
    java.util.List<CaseDto> result =
        service.searchCases(
            "admin",
            com.example.legacy.service.dto.CaseSearchCriteriaDto.builder()
                .query(" Request ")
                .status("NEW")
                .assignee("worker")
                .page(2)
                .build());
    assertEquals("Found", result.get(0).getTitle());
    verify(transaction).commit();
  }

  /**
   * Builds detached case details without loading document payloads.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void buildsDetachedCaseDetailsWithoutLoadingDocumentPayloads() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setStatus("NEW");
    item.setCreatedBy("creator");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    com.example.legacy.model.CaseNote note = new com.example.legacy.model.CaseNote();
    note.setBody("Note");
    when(notes.findNotesByCaseId(session, 7L))
        .thenReturn(java.util.Collections.singletonList(note));
    com.example.legacy.model.CaseDocument document =
        mock(com.example.legacy.model.CaseDocument.class);
    when(document.getOriginalName()).thenReturn("note.txt");
    when(documents.findDocumentsByCaseId(session, 7L))
        .thenReturn(java.util.Collections.singletonList(document));
    when(access.listAllAccounts(session)).thenReturn(java.util.Collections.singletonList(admin));
    com.example.legacy.service.dto.CaseDetails result =
        service.getCaseDetails("admin", RecordReferenceDto.builder().id(7).build());
    assertEquals("Note", result.getNotes().get(0).getBody());
    assertEquals("note.txt", result.getDocuments().get(0).getOriginalName());
    assertNull(result.getDocuments().get(0).getPayload());
    assertTrue(result.isMayEdit());
    assertTrue(result.isMayComment());
    assertFalse(result.getNextStates().isEmpty());
    verify(document, never()).getPayload();
  }

  /**
   * Updates current versions without replacing identity or category.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void updatesCurrentVersionsWithoutReplacingIdentityOrCategory() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setStatus("NEW");
    item.setVersion(3);
    item.setCategory("ACCESS");
    item.setCreatedBy("creator");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    CaseDto result =
        service.updateCase(
            "admin",
            UpdateCaseDto.builder()
                .id(7)
                .version(3)
                .title("Changed")
                .description("Changed description")
                .priority("LOW")
                .build());
    assertEquals("Changed", result.getTitle());
    assertEquals("ACCESS", result.getCategory());
    assertEquals("creator", result.getCreatedBy());
    assertNotNull(result.getUpdatedAt());
    verify(cases).flush(session);
    verify(transaction).commit();
  }

  /**
   * Rejects archived cases and closed case reassignment.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsArchivedCasesAndClosedCaseReassignment() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setArchived(true);
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    assertThrows(
        RecordNotFoundException.class,
        () -> service.getCaseDetails("admin", RecordReferenceDto.builder().id(7).build()));
    item.setArchived(false);
    item.setStatus("CLOSED");
    when(access.requireActiveAccount(session, "worker")).thenReturn(new Account());
    assertThrows(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () ->
            service.assignCase("admin", AssignCaseDto.builder().id(7).assignee("worker").build()));
    verifyNoInteractions(audit, notifications);
  }

  /**
   * Closes cases and notifies creator and other assignees.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void closesCasesAndNotifiesCreatorAndOtherAssignees() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setStatus("APPROVED");
    item.setCreatedBy("creator");
    item.setAssignedTo("worker");
    item.setCaseNumber("CASE-7");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    service.transitionCase(
        "admin",
        com.example.legacy.service.dto.TransitionCaseDto.builder()
            .id(7)
            .destination("CLOSED")
            .build());
    assertEquals("CLOSED", item.getStatus());
    assertNotNull(item.getClosedAt());
    verify(notifications)
        .createNotification(eq(session), eq("creator"), eq(7L), anyString(), anyString());
    verify(notifications)
        .createNotification(eq(session), eq("worker"), eq(7L), anyString(), anyString());
  }

  /**
   * Avoids notifying the actor twice during normal transitions.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void avoidsNotifyingTheActorTwiceDuringNormalTransitions() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setStatus("NEW");
    item.setCreatedBy("creator");
    item.setAssignedTo("admin");
    item.setCaseNumber("CASE-7");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    service.transitionCase(
        "admin",
        com.example.legacy.service.dto.TransitionCaseDto.builder()
            .id(7)
            .destination("TRIAGED")
            .build());
    assertEquals("TRIAGED", item.getStatus());
    assertNull(item.getClosedAt());
    verify(notifications, times(1))
        .createNotification(eq(session), eq("creator"), eq(7L), anyString(), anyString());
  }

  /**
   * Records comments and notifies only other assigned workers.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void recordsCommentsAndNotifiesOnlyOtherAssignedWorkers() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setCreatedBy("creator");
    item.setAssignedTo("worker");
    item.setCaseNumber("CASE-7");
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    service.addCaseComment(
        "admin",
        com.example.legacy.service.dto.AddCaseCommentDto.builder().id(7).body(" Comment ").build());
    ArgumentCaptor<com.example.legacy.model.CaseNote> saved =
        ArgumentCaptor.forClass(com.example.legacy.model.CaseNote.class);
    verify(notes).save(eq(session), saved.capture());
    assertEquals("Comment", saved.getValue().getBody());
    assertEquals("admin", saved.getValue().getAuthor());
    assertNotNull(saved.getValue().getCreatedAt());
    verify(notifications)
        .createNotification(eq(session), eq("worker"), eq(7L), anyString(), anyString());
    item.setAssignedTo("admin");
    service.addCaseComment(
        "admin",
        com.example.legacy.service.dto.AddCaseCommentDto.builder().id(7).body("Another").build());
    verifyNoMoreInteractions(notifications);
  }

  /**
   * Archives cases with administrator authorization and audit.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void archivesCasesWithAdministratorAuthorizationAndAudit() {
    stubAuthenticatedTransaction();
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    when(cases.findCaseById(session, 7L)).thenReturn(item);
    service.archiveCase("admin", RecordReferenceDto.builder().id(7).build());
    assertTrue(item.isArchived());
    assertNotNull(item.getUpdatedAt());
    verify(access).requireAdministratorPermission(admin);
    verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.CASE),
            eq(7L),
            eq(AuditAction.ARCHIVE),
            eq("admin"),
            anyString());
  }

  /**
   * Starts one mocked unit of work for an authenticated actor.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void stubAuthenticatedTransaction() {
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
    when(access.requireActiveAccount(session, "admin")).thenReturn(admin);
  }

  /**
   * Builds creation input with independent immutable field values.
   *
   * @return the create case DTO
   */
  private CreateCaseDto createCaseRequest() {
    return CreateCaseDto.builder()
        .title("Case title")
        .description("Description")
        .category("GENERAL")
        .priority("MEDIUM")
        .department("Operations")
        .build();
  }

  /**
   * Models generated database fields without executing SQL in a unit test.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void stubGeneratedCaseId() {
    doAnswer(
            call -> {
              ServiceCase item = call.getArgument(1);
              item.setId(7L);
              item.setVersion(0);
              return null;
            })
        .when(cases)
        .save(eq(session), any(ServiceCase.class));
  }
}
