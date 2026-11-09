package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.model.Account;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuthenticationService;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.PasswordsUtil;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Verifies credential normalization and response redaction with mocked persistence access.
 *
 * @author Chuck Emmons
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AuthenticationServiceTest {
  @Mock private SessionFactory factory;
  @Mock private Session session;
  @Mock private Transaction transaction;
  @Mock private AccountAccessService access;
  private AuthenticationService service;

  /**
   * Creates a fresh authentication service and transaction scope for each test.
   *
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Before
  public void setUp() {
    service =
        new AuthenticationService(
            new HibernateTransactionTemplate(factory), access, new AccountMapper());
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
  }

  /**
   * Treats absent accounts as a normal {@code null} result and still commits/closes.
   *
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
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
  public void returnsNoAccountWhenAccountIsMissing() {
    AuthDto result =
        service.authenticate(AuthDto.builder().username("missing").password("unused").build());
    assertNull(result.getAccount());
    assertNull(result.getPassword());
    verify(transaction).commit();
    verify(session).close();
  }

  /**
   * Normalizes usernames, verifies the password, and excludes credentials from the returned
   * snapshot.
   *
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
  public void normalizesCredentialsAndRedactsResponse() {
    Account account = new Account();
    account.setUsername("worker");
    account.setEnabled(true);
    account.setRole("WORKER");
    account.setPasswordHash(PasswordsUtil.hashPassword("TestPassword!2026"));
    when(access.findAccountByUsername(session, "worker")).thenReturn(account);
    AuthDto input = AuthDto.builder().username(" WORKER ").password("TestPassword!2026").build();
    AuthDto result = service.authenticate(input);
    assertEquals("worker", result.getAccount().getUsername());
    assertNull(result.getPassword());
    assertEquals("TestPassword!2026", input.getPassword());
    verify(access).findAccountByUsername(session, "worker");
    verify(transaction).commit();
    verify(session).close();
  }
}
