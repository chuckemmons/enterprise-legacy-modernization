package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.Account;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.CasePolicyService;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.unit.support.ServiceTestSupport;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for AccountAccessService.
 *
 * @author Chuck Emmons
 */
public class AccountAccessServiceTest extends ServiceTestSupport {

  private AccountDao accounts;
  private AccountAccessService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    accounts = mock(AccountDao.class);
    service =
        new AccountAccessService(template, accounts, new AccountMapper(), new CasePolicyService());
  }

  /**
   * Reloads active accounts in its own transaction.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reloadsActiveAccountsInItsOwnTransaction() {
    when(accounts.findAccountByUsername(session, "worker")).thenReturn(account("worker", "WORKER"));
    assertEquals("worker", service.loadActiveAccount("worker").getUsername());
    verify(transaction).commit();
    verify(session).close();
    assertEquals("worker", service.requireActiveAccount(session, "worker").getUsername());
    when(accounts.findAllAccounts(session))
        .thenReturn(Collections.singletonList(account("worker", "WORKER")));
    assertEquals(1, service.listAllAccounts(session).size());
  }

  /**
   * Rejects missing and disabled accounts.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsMissingAndDisabledAccounts() {
    assertThrows(
        AccessDeniedException.class, () -> service.requireActiveAccount(session, "missing"));
    Account disabled = account("disabled", "WORKER");
    disabled.setEnabled(false);
    when(accounts.findAccountByUsername(session, "disabled")).thenReturn(disabled);
    assertThrows(
        AccessDeniedException.class, () -> service.requireActiveAccount(session, "disabled"));
  }

  /**
   * Enforces each role boundary.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void enforcesEachRoleBoundary() {
    service.requireSupervisorPermission(account("admin", "ADMIN"));
    service.requireSupervisorPermission(account("boss", "SUPERVISOR"));
    assertThrows(
        AccessDeniedException.class,
        () -> service.requireSupervisorPermission(account("worker", "WORKER")));
    service.requireAdministratorPermission(account("admin", "ADMIN"));
    service.requireCaseWorkPermission(account("worker", "WORKER"));
    for (String role : new String[] {"ADMIN", "SUPERVISOR", "AUDITOR"}) {
      service.requireReportingPermission(account("user", role));
    }
    assertThrows(
        AccessDeniedException.class,
        () -> service.requireReportingPermission(account("worker", "WORKER")));
    assertThrows(
        AccessDeniedException.class,
        () -> service.requireAdministratorPermission(account("auditor", "AUDITOR")));
    assertThrows(
        AccessDeniedException.class,
        () -> service.requireCaseWorkPermission(account("auditor", "AUDITOR")));
  }
}
