package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.dao.CaseDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.model.Account;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuditService;
import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.SummaryMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.PasswordsUtil;
import java.util.Collections;
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
 * Tests account/audit atomic orchestration and grouped reporting without implementing a fake SQL
 * engine.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BackOfficeServiceTest {
  @Mock private SessionFactory factory;
  @Mock private Session session;
  @Mock private Transaction transaction;
  @Mock private AccountDao accounts;
  @Mock private CaseDao cases;
  @Mock private AccountAccessService access;
  @Mock private AuditService audit;
  private BackOfficeService service;
  private Account admin;

  /**
   * Creates real mapping collaborators and one isolated mocked transaction for each use case.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Before
  public void setUp() {
    service =
        new BackOfficeService(
            new HibernateTransactionTemplate(factory),
            accounts,
            cases,
            access,
            audit,
            new AccountMapper(),
            new CaseMapper(),
            new SummaryMapper());
    admin = new Account();
    admin.setUsername("admin");
    admin.setRole("ADMIN");
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
    when(access.requireActiveAccount(session, "admin")).thenReturn(admin);
  }

  /**
   * Commits account and audit work on one session while leaving immutable credentials unchanged.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if a required runtime service cannot complete the operation
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void createsAccountAndAuditOnOneSession() {
    stubGeneratedAccountId();
    CreateAccountDto request = createAccountRequest();
    service.createUser("admin", request);
    ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(eq(session), saved.capture());
    assertTrue(
        PasswordsUtil.matchesPassword(request.getPassword(), saved.getValue().getPasswordHash()));
    assertEquals("TestPassword!2026", request.getPassword());
    verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.ACCOUNT),
            eq(42L),
            eq(AuditAction.CREATE),
            eq("admin"),
            anyString());
    InOrder order = inOrder(accounts, audit, transaction, session);
    order.verify(accounts).save(eq(session), any(Account.class));
    order.verify(accounts).flush(session);
    order
        .verify(audit)
        .recordAuditEvent(
            eq(session),
            any(AuditEntity.class),
            eq(42L),
            any(AuditAction.class),
            eq("admin"),
            anyString());
    order.verify(transaction).commit();
    order.verify(session).close();
  }

  /**
   * Rolls back account work if its associated audit operation fails.
   *
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rollsBackFailedAuditWrite() {
    stubGeneratedAccountId();
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
            RuntimeException.class, () -> service.createUser("admin", createAccountRequest())));
    verify(transaction).rollback();
    verify(transaction, never()).commit();
    verify(session).close();
  }

  /**
   * Performs all report queries and permission checks inside the same transaction.
   *
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void readsReportInOneTransaction() {
    when(cases.countCasesByGrouping(session, ReportGrouping.STATUS))
        .thenReturn(Collections.emptyList());
    when(cases.countCasesByGrouping(session, ReportGrouping.PRIORITY))
        .thenReturn(Collections.emptyList());
    when(cases.countOverdueCases(session)).thenReturn(0L);
    assertEquals(Long.valueOf(0), service.getReportSummary("admin").getOverdue());
    verify(access).requireReportingPermission(admin);
    verify(transaction).commit();
    verify(session).close();
    verifyNoInteractions(accounts, audit);
  }

  /**
   * Rejects duplicate usernames before saving.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsDuplicateUsernamesBeforeSaving() {
    when(access.findAccountByUsername(session, "worker1")).thenReturn(new Account());
    assertThrows(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> service.createUser("admin", createAccountRequest()));
    verifyNoInteractions(accounts, audit);
    verify(transaction).rollback();
  }

  /**
   * Changes roles and rejects unknown or self owned accounts.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void changesRolesAndRejectsUnknownOrSelfOwnedAccounts() {
    Account worker = new Account();
    worker.setUsername("worker");
    worker.setRole("WORKER");
    when(accounts.findAccountById(session, 7)).thenReturn(worker);
    com.example.legacy.service.dto.ChangeAccountRoleDto change =
        com.example.legacy.service.dto.ChangeAccountRoleDto.builder().id(7).role("AUDITOR").build();
    service.changeUserRole("admin", change);
    assertEquals("AUDITOR", worker.getRole());
    verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.ACCOUNT),
            eq(7L),
            eq(AuditAction.ROLE_CHANGE),
            eq("admin"),
            contains("WORKER"));
    worker.setUsername("admin");
    assertThrows(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> service.changeUserRole("admin", change));
    assertThrows(
        com.example.legacy.service.exception.RecordNotFoundException.class,
        () ->
            service.changeUserRole(
                "admin",
                com.example.legacy.service.dto.ChangeAccountRoleDto.builder()
                    .id(99)
                    .role("WORKER")
                    .build()));
  }

  /**
   * Toggles enabled state and protects the actors own account.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void togglesEnabledStateAndProtectsTheActorsOwnAccount() {
    Account worker = new Account();
    worker.setUsername("worker");
    worker.setEnabled(true);
    when(accounts.findAccountById(session, 7)).thenReturn(worker);
    com.example.legacy.service.dto.RecordReferenceDto reference =
        com.example.legacy.service.dto.RecordReferenceDto.builder().id(7).build();
    service.toggleUserEnabled("admin", reference);
    assertFalse(worker.isEnabled());
    service.toggleUserEnabled("admin", reference);
    assertTrue(worker.isEnabled());
    worker.setUsername("admin");
    assertThrows(
        com.example.legacy.service.exception.BusinessValidationException.class,
        () -> service.toggleUserEnabled("admin", reference));
    assertThrows(
        com.example.legacy.service.exception.RecordNotFoundException.class,
        () ->
            service.toggleUserEnabled(
                "admin",
                com.example.legacy.service.dto.RecordReferenceDto.builder().id(99).build()));
  }

  /**
   * Lists accounts and builds dashboard without reporting only queries.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void listsAccountsAndBuildsDashboardWithoutReportingOnlyQueries() {
    when(accounts.findAllAccounts(session)).thenReturn(Collections.singletonList(admin));
    assertEquals("admin", service.listAllAccounts("admin").get(0).getUsername());
    when(cases.countCasesByGrouping(session, ReportGrouping.STATUS))
        .thenReturn(Collections.singletonList(new Object[] {"NEW", 2L}));
    when(cases.countOverdueCases(session)).thenReturn(1L);
    com.example.legacy.service.dto.CaseSummary result = service.getDashboardSummary("admin");
    assertEquals("NEW", result.getStatuses().get(0).getLabel());
    assertTrue(result.getPriorities().isEmpty());
    assertEquals(Long.valueOf(1), result.getOverdue());
    verify(access, never()).requireReportingPermission(admin);
    verify(cases, never()).countCasesByGrouping(session, ReportGrouping.PRIORITY);
  }

  /**
   * Bounds exports and requires reporting permission.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void boundsExportsAndRequiresReportingPermission() {
    when(cases.searchCases(
            session, null, null, null, 0, com.example.legacy.util.ValidationLimits.EXPORT_ROWS))
        .thenReturn(Collections.emptyList());
    assertTrue(service.exportCases("admin").isEmpty());
    verify(access).requireReportingPermission(admin);
  }

  /**
   * Creates an account command without exposing a mutable form or persistent entity.
   *
   * @return the create account DTO
   */
  private CreateAccountDto createAccountRequest() {
    return CreateAccountDto.builder()
        .username("worker1")
        .displayName("Worker")
        .role("WORKER")
        .department("Operations")
        .password("TestPassword!2026")
        .build();
  }

  /**
   * Supplies the generated identity expected by the account/audit use case.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void stubGeneratedAccountId() {
    doAnswer(
            call -> {
              ((Account) call.getArgument(1)).setId(42L);
              return null;
            })
        .when(accounts)
        .save(eq(session), any(Account.class));
  }
}
