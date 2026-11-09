package com.example.legacy.integration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.AuditDao;
import com.example.legacy.model.AuditEvent;
import java.util.Date;
import org.junit.Test;

/**
 * Exercises audit persistence, descending history ordering, and bounded pagination on seeded H2
 * data.
 *
 * @author Chuck Emmons
 */
public class AuditDaoIntegrationTest extends H2DaoTestSupport {
  private final AuditDao dao = new AuditDao();

  /**
   * Returns recent events newest first and clamps negative page indices.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void readsSeededAuditHistory() {
    assertEquals(Long.valueOf(31), dao.findRecentAuditEvents(session, 0).get(0).getId());
    assertEquals(2, dao.findRecentAuditEvents(session, -1).size());
    assertTrue(dao.findRecentAuditEvents(session, 1).isEmpty());
  }

  /**
   * Persists events and enforces the configured 50-row page size.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesAndPaginatesAuditEvents() {
    for (int i = 0; i < 51; i++) {
      AuditEvent event = new AuditEvent();
      event.setEntityType("CASE");
      event.setEntityId(1L);
      event.setActionName("UPDATE");
      event.setActor("worker");
      event.setDetails("Event " + i);
      event.setCreatedAt(new Date(System.currentTimeMillis() + i));
      dao.save(session, event);
    }
    flushAndClear();
    assertEquals(50, dao.findRecentAuditEvents(session, 0).size());
    assertEquals(3, dao.findRecentAuditEvents(session, 1).size());
  }
}
