package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.model.ServiceCase;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for CaseDao.
 *
 * @author Chuck Emmons
 */
public class CaseDaoTest {

  /**
   * Binds optional filters and clamps negative pagination.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void bindsOptionalFiltersAndClampsNegativePagination() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    List<ServiceCase> matches = Collections.singletonList(new ServiceCase());
    when(query.list()).thenReturn(matches);
    CaseDao dao = new CaseDao();
    assertSame(matches, dao.searchCases(session, "request", "NEW", "worker", 2, 25));
    verify(query).setString("q", "%request%");
    verify(query).setString("status", "NEW");
    verify(query).setString("assignee", "worker");
    verify(query).setFirstResult(50);
    verify(query).setMaxResults(25);
    dao.searchCases(session, " ", "", "", -1, 25);
    verify(query).setString("q", null);
    verify(query).setString("status", null);
    verify(query).setString("assignee", null);
    verify(query).setFirstResult(0);
    dao.searchCases(session, null, null, null, 0, 25);
    verify(query, times(2)).setString("q", null);
  }

  /**
   * Uses only fixed grouping queries and rejects missing grouping.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void usesOnlyFixedGroupingQueriesAndRejectsMissingGrouping() {
    Session session = mock(Session.class);
    Query query = mock(Query.class);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    when(query.list()).thenReturn(Collections.emptyList());
    CaseDao dao = new CaseDao();
    assertTrue(dao.countCasesByGrouping(session, ReportGrouping.STATUS).isEmpty());
    assertTrue(dao.countCasesByGrouping(session, ReportGrouping.PRIORITY).isEmpty());
    verify(session).getNamedQuery("ServiceCase.countByStatus");
    verify(session).getNamedQuery("ServiceCase.countByPriority");
    assertThrows(NullPointerException.class, () -> dao.countCasesByGrouping(session, null));
  }

  /**
   * Bounds overdue notifications and leaves transaction ownership to the caller.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void boundsOverdueNotificationsAndLeavesTransactionOwnershipToTheCaller() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    when(query.list()).thenReturn(Collections.emptyList());
    when(query.uniqueResult()).thenReturn(3L);
    CaseDao dao = new CaseDao();
    assertEquals(Long.valueOf(3), dao.countOverdueCases(session));
    assertTrue(dao.findOverdueCasesAwaitingNotification(session).isEmpty());
    verify(query, times(2)).setString("closedStatus", "CLOSED");
    verify(query, times(2)).setTimestamp(eq("now"), any(Date.class));
    verify(query).setMaxResults(300);
    verify(session, never()).beginTransaction();
    ServiceCase item = new ServiceCase();
    when(session.get(ServiceCase.class, 7L)).thenReturn(item);
    assertSame(item, dao.findCaseById(session, 7));
    dao.save(session, item);
    dao.flush(session);
    verify(session).save(item);
    verify(session).flush();
  }
}
