package com.example.legacy.integration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.Account;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

/**
 * Exercises account lookup, ordering, generated identities, and uniqueness using seeded H2 data.
 *
 * @author Chuck Emmons
 */
public class AccountDaoIntegrationTest extends H2DaoTestSupport {
  private final AccountDao dao = new AccountDao();

  /**
   * Finds exact usernames and identities, including disabled accounts.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void findsSeededAccounts() {
    assertEquals(Long.valueOf(1), dao.findAccountByUsername(session, "admin").getId());
    assertEquals("worker", dao.findAccountById(session, 2).getUsername());
    assertFalse(dao.findAccountByUsername(session, "disabled").isEnabled());
    assertNull(dao.findAccountByUsername(session, "ADMIN"));
    assertNull(dao.findAccountById(session, -1));
  }

  /**
   * Includes disabled users and applies the DAO's configured username ordering.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void listsAllAccountsInOrder() {
    assertEquals(
        Arrays.asList("admin", "auditor", "disabled", "supervisor", "worker"),
        dao.findAllAccounts(session).stream()
            .map(Account::getUsername)
            .collect(Collectors.toList()));
  }

  /**
   * Writes and reloads a generated account after clearing the managed-object cache.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesAccount() {
    Account account = createAccount("new_worker");
    dao.save(session, account);
    dao.flush(session);
    session.clear();
    assertNotNull(account.getId());
    assertEquals("New worker", dao.findAccountById(session, account.getId()).getDisplayName());
  }

  /**
   * Verifies the actual H2 unique constraint rather than simulating a duplicate lookup.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rejectsDuplicateUsername() {
    dao.save(session, createAccount("admin"));
    assertThrows(ConstraintViolationException.class, () -> dao.flush(session));
  }

  /**
   * Creates a complete new persistence bean for write/constraint checks.
   *
   * @param username the username identifying the account
   * @return the account
   */
  private Account createAccount(String username) {
    Account account = new Account();
    account.setUsername(username);
    account.setDisplayName("New worker");
    account.setPasswordHash("test-only-hash");
    account.setRole("WORKER");
    account.setDepartment("Operations");
    account.setEnabled(true);
    account.setCreatedAt(new Date());
    return account;
  }
}
