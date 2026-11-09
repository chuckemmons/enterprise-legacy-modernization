package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.model.Account;
import java.util.Collections;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for AccountDao.
 *
 * @author Chuck Emmons
 */
public class AccountDaoTest {

  /**
   * Binds usernames and keeps session ownership with the caller.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void bindsUsernamesAndKeepsSessionOwnershipWithTheCaller() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    Account user = new Account();
    when(session.getNamedQuery(anyString())).thenReturn(query);
    when(query.uniqueResult()).thenReturn(user);
    when(query.list()).thenReturn(Collections.singletonList(user));
    when(session.get(Account.class, 7L)).thenReturn(user);
    AccountDao dao = new AccountDao();
    assertSame(user, dao.findAccountByUsername(session, "worker' or 1=1 --"));
    verify(query).setString("u", "worker' or 1=1 --");
    assertSame(user, dao.findAccountById(session, 7));
    assertEquals(Collections.singletonList(user), dao.findAllAccounts(session));
    dao.save(session, user);
    dao.flush(session);
    verify(session).save(user);
    verify(session).flush();
    verify(session, never()).beginTransaction();
  }
}
