package com.example.legacy.dao;

import com.example.legacy.model.Account;
import java.util.List;
import org.hibernate.Session;

/**
 * All user-table database access lives in this DAO.
 *
 * @author Chuck Emmons
 */
public class AccountDao {
  /**
   * Returns the account matching the exact username or {@code null} if absent.
   *
   * @param session the Hibernate session used for persistence operations
   * @param username the username identifying the account
   * @return the account matching the exact username or {@code null} if absent
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public Account findAccountByUsername(Session session, String username) {
    return (Account)
        session.getNamedQuery("Account.findByUsername").setString("u", username).uniqueResult();
  }

  /**
   * Returns the account with the supplied identifier or {@code null} if absent; authorization and
   * visibility checks belong to the caller.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the account with the supplied identifier or {@code null} if absent; authorization and
   *     visibility checks belong to the caller
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public Account findAccountById(Session session, long id) {
    return (Account) session.get(Account.class, id);
  }

  /**
   * Returns all accounts ordered by username, including disabled accounts.
   *
   * @param session the Hibernate session used for persistence operations
   * @return all accounts ordered by username, including disabled accounts
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<Account> findAllAccounts(Session session) {
    return session.getNamedQuery("Account.findAll").list();
  }

  /**
   * Schedules a new account for persistence in the current session; the caller owns flushing and
   * committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param account the account to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, Account account) {
    session.save(account);
  }

  /**
   * Flushes pending session changes to the database without committing the caller's transaction.
   *
   * @param session the Hibernate session used for persistence operations
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void flush(Session session) {
    session.flush();
  }
}
