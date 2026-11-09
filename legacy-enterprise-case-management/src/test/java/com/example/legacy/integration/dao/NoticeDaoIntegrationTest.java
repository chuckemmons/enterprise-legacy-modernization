package com.example.legacy.integration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.CaseNotice;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Tests recipient-scoped notification history, pending delivery order, and persistence using H2.
 *
 * @author Chuck Emmons
 */
public class NoticeDaoIntegrationTest extends H2DaoTestSupport {
  private final NoticeDao dao = new NoticeDao();

  /**
   * Includes read notices but never returns another recipient's history.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void scopesHistoryToRecipient() {
    assertEquals(
        Arrays.asList(42L, 40L),
        dao.findNotificationsByUsername(session, "worker").stream()
            .map(CaseNotice::getId)
            .collect(Collectors.toList()));
    assertTrue(dao.findNotificationsByUsername(session, "unknown").isEmpty());
    assertNull(dao.findNotificationById(session, -1));
  }

  /**
   * Selects only unsent notices in oldest-first delivery order.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void selectsPendingDeliveryIds() {
    assertEquals(Arrays.asList(40L, 42L, 43L), dao.findPendingEmailNotificationIds(session));
  }

  /**
   * Persists and reloads a generated notice and its read state.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesAndUpdatesNotice() {
    CaseNotice notice = new CaseNotice();
    notice.setRecipient("worker");
    notice.setCaseId(1L);
    notice.setSubject("New notice");
    notice.setMessage("Message");
    notice.setCreatedAt(new Date());
    dao.save(session, notice);
    flushAndClear();
    CaseNotice loaded = dao.findNotificationById(session, notice.getId());
    assertEquals("New notice", loaded.getSubject());
    loaded.setRead(true);
    flushAndClear();
    assertTrue(dao.findNotificationById(session, notice.getId()).isRead());
  }
}
