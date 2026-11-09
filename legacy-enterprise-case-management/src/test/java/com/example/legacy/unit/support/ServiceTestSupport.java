package com.example.legacy.unit.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.legacy.model.Account;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;

/**
 * Supplies isolated Hibernate lifecycle collaborators without opening a database connection.
 *
 * @author Chuck Emmons
 */
public abstract class ServiceTestSupport {
  /** Test fixture for factory. */
  protected SessionFactory factory;

  /** Test fixture for session. */
  protected Session session;

  /** Test fixture for transaction. */
  protected Transaction transaction;

  /** Test fixture for template. */
  protected HibernateTransactionTemplate template;

  /** Creates independent session and transaction mocks for each test. */
  @Before
  public void createTransactionCollaborators() {
    factory = mock(SessionFactory.class);
    session = mock(Session.class);
    transaction = mock(Transaction.class);
    when(factory.openSession()).thenReturn(session);
    when(session.beginTransaction()).thenReturn(transaction);
    template = new HibernateTransactionTemplate(factory);
  }

  /**
   * Creates an active actor for permission and ownership scenarios.
   *
   * @param username the actor identifier
   * @param role the actor role
   * @return an active account with the supplied identity and role
   */
  protected Account account(String username, String role) {
    Account value = new Account();
    value.setUsername(username);
    value.setRole(role);
    value.setEnabled(true);
    return value;
  }
}
