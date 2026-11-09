package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.AuditDao;
import com.example.legacy.model.AuditEvent;
import java.util.Collections;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for AuditDao.
 *
 * @author Chuck Emmons
 */
public class AuditDaoTest {

  /**
   * Bounds audit pages and persists in the supplied session.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void boundsAuditPagesAndPersistsInTheSuppliedSession() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    AuditEvent event = new AuditEvent();
    when(query.list()).thenReturn(Collections.singletonList(event));
    AuditDao dao = new AuditDao();
    assertEquals(Collections.singletonList(event), dao.findRecentAuditEvents(session, -1));
    verify(query).setFirstResult(0);
    verify(query).setMaxResults(com.example.legacy.util.ValidationLimits.AUDIT_PAGE_SIZE);
    dao.findRecentAuditEvents(session, 2);
    verify(query).setFirstResult(2 * com.example.legacy.util.ValidationLimits.AUDIT_PAGE_SIZE);
    dao.save(session, event);
    verify(session).save(event);
  }
}
