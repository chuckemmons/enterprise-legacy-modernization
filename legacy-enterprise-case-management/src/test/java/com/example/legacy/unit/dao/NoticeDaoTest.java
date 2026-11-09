package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.CaseNotice;
import java.util.Collections;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for NoticeDao.
 *
 * @author Chuck Emmons
 */
public class NoticeDaoTest {

  /**
   * Scopes recipient lists and bounds pending delivery batches.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void scopesRecipientListsAndBoundsPendingDeliveryBatches() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    CaseNotice notice = new CaseNotice();
    when(query.list()).thenReturn(Collections.singletonList(notice));
    when(session.get(CaseNotice.class, 9L)).thenReturn(notice);
    NoticeDao dao = new NoticeDao();
    assertEquals(
        Collections.singletonList(notice), dao.findNotificationsByUsername(session, "worker"));
    verify(query).setString("user", "worker");
    verify(query).setMaxResults(100);
    assertSame(notice, dao.findNotificationById(session, 9));
    dao.save(session, notice);
    verify(session).save(notice);
    when(query.list()).thenReturn(Collections.singletonList(9L));
    assertEquals(Collections.singletonList(9L), dao.findPendingEmailNotificationIds(session));
    verify(query).setMaxResults(50);
  }
}
