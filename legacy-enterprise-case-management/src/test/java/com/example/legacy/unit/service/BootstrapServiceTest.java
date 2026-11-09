package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.Account;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.BootstrapService;
import com.example.legacy.service.CaseService;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.unit.support.ServiceTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavioral contracts for BootstrapService.
 *
 * @author Chuck Emmons
 */
public class BootstrapServiceTest extends ServiceTestSupport {

  private AccountDao accounts;
  private AccountAccessService access;
  private CaseService cases;
  private BootstrapService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    accounts = mock(AccountDao.class);
    access = mock(AccountAccessService.class);
    cases = mock(CaseService.class);
    service = new BootstrapService(template, accounts, new AccountMapper(), access, cases);
  }

  /**
   * Rejects incomplete seed credentials before persistence.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rejectsIncompleteSeedCredentialsBeforePersistence() {
    assertThrows(
        BusinessValidationException.class, () -> service.seedAdmin(null, "ValidPassword!2026"));
    assertThrows(
        BusinessValidationException.class,
        () -> service.seedAdmin("invalid user", "ValidPassword!2026"));
    assertThrows(BusinessValidationException.class, () -> service.seedAdmin("admin", null));
    assertThrows(BusinessValidationException.class, () -> service.seedAdmin("admin", "short"));
    assertThrows(BusinessValidationException.class, () -> service.seedDemo(null));
    verifyNoInteractions(factory, accounts, cases);
  }

  /**
   * Creates hashed administrator but never overwrites an existing account.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void createsHashedAdministratorButNeverOverwritesAnExistingAccount() {
    service.seedAdmin("admin", "ValidPassword!2026");
    ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(eq(session), saved.capture());
    assertEquals("ADMIN", saved.getValue().getRole());
    assertTrue(saved.getValue().isEnabled());
    assertTrue(
        com.example.legacy.util.PasswordsUtil.matchesPassword(
            "ValidPassword!2026", saved.getValue().getPasswordHash()));
    assertNotNull(saved.getValue().getCreatedAt());
    when(access.findAccountByUsername(session, "admin")).thenReturn(account("admin", "ADMIN"));
    assertThrows(
        IllegalStateException.class, () -> service.seedAdmin("admin", "ValidPassword!2026"));
    verify(accounts, times(1)).save(eq(session), any(Account.class));
  }

  /**
   * Seeds only missing demo accounts and assigns every created case.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void seedsOnlyMissingDemoAccountsAndAssignsEveryCreatedCase() {
    when(access.findAccountByUsername(session, "supervisor"))
        .thenReturn(account("supervisor", "SUPERVISOR"));
    when(cases.createCase(eq("worker"), any(CreateCaseDto.class)))
        .thenAnswer(call -> CaseDto.builder().id(7L).build());
    service.seedDemo("ValidPassword!2026");
    ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
    verify(accounts, times(2)).save(eq(session), saved.capture());
    assertEquals("worker", saved.getAllValues().get(0).getUsername());
    assertEquals("AUDITOR", saved.getAllValues().get(1).getRole());
    ArgumentCaptor<CreateCaseDto> requests = ArgumentCaptor.forClass(CreateCaseDto.class);
    verify(cases, times(8)).createCase(eq("worker"), requests.capture());
    assertEquals("ACCESS", requests.getAllValues().get(0).getCategory());
    assertEquals("HIGH", requests.getAllValues().get(0).getPriority());
    assertEquals("MEDIUM", requests.getAllValues().get(1).getPriority());
    assertNotNull(requests.getAllValues().get(0).getDueAt());
    verify(cases, times(8)).assignCase(eq("supervisor"), any(AssignCaseDto.class));
  }
}
