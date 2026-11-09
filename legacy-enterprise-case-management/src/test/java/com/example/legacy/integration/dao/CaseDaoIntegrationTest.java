package com.example.legacy.integration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.model.ServiceCase;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Exercises real case filtering, pagination, grouping, overdue queries, persistence, and
 * versioning.
 *
 * @author Chuck Emmons
 */
public class CaseDaoIntegrationTest extends H2DaoTestSupport {
  private final CaseDao dao = new CaseDao();

  /**
   * Applies search/status/assignee parameters and excludes archived cases.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void filtersSeededCases() {
    assertEquals(
        Collections.singletonList(1L),
        collectCaseIds(dao.searchCases(session, "access", "NEW", "worker", 0, 25)));
    assertEquals(3, dao.searchCases(session, null, null, "worker", 0, 25).size());
    assertTrue(dao.searchCases(session, null, "NEW' or 1=1 --", null, 0, 25).isEmpty());
    assertEquals(4, dao.searchCases(session, null, null, null, 0, 25).size());
  }

  /**
   * Keeps empty filters optional and treats case-number searches and hostile input as bound values.
   *
   * @throws AssertionError if optional filters change results or query text is interpreted as SQL
   * @throws org.hibernate.HibernateException if executing a case search fails
   */
  @Test
  public void bindsOptionalSearchFiltersWithoutChangingQueryStructure() {
    assertEquals(
        Arrays.asList(5L, 3L, 2L, 1L),
        collectCaseIds(dao.searchCases(session, " \t", "", "", 0, 25)));
    assertEquals(
        Collections.singletonList(1L),
        collectCaseIds(dao.searchCases(session, "case-000001", null, null, 0, 25)));
    assertTrue(dao.searchCases(session, "' or 1=1 --", null, null, 0, 25).isEmpty());
    assertTrue(dao.searchCases(session, null, null, "worker' or 1=1 --", 0, 25).isEmpty());
  }

  /**
   * Groups priorities using the fixed reporting query and excludes archived rows.
   *
   * @throws AssertionError if priority grouping changes its counts or ordering
   * @throws org.hibernate.HibernateException if executing the grouping query fails
   */
  @Test
  public void groupsPrioritiesUsingTheConfiguredReportingQuery() {
    List<Object[]> rows = dao.countCasesByGrouping(session, ReportGrouping.PRIORITY);
    assertEquals(
        Arrays.asList("CRITICAL", "HIGH", "LOW", "MEDIUM"),
        rows.stream().map(row -> (String) row[0]).collect(Collectors.toList()));
    for (Object[] row : rows) {
      assertEquals(1L, ((Number) row[1]).longValue());
    }
    assertThrows(NullPointerException.class, () -> dao.countCasesByGrouping(session, null));
  }

  /**
   * Returns stable nonoverlapping pages ordered by latest update.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void paginatesAndOrders() {
    assertEquals(
        Arrays.asList(5L, 3L), collectCaseIds(dao.searchCases(session, null, null, null, 0, 2)));
    assertEquals(
        Arrays.asList(2L, 1L), collectCaseIds(dao.searchCases(session, null, null, null, 1, 2)));
    assertEquals(
        Arrays.asList(5L, 3L), collectCaseIds(dao.searchCases(session, null, null, null, -1, 2)));
  }

  /**
   * Groups only nonarchived rows and counts open overdue cases independently of notice state.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void groupsAndCountsOverdue() {
    Map<String, Long> statuses = new HashMap<>();
    for (Object[] row : dao.countCasesByGrouping(session, ReportGrouping.STATUS)) {
      statuses.put((String) row[0], ((Number) row[1]).longValue());
    }
    assertEquals(4, statuses.size());
    assertEquals(Long.valueOf(1), statuses.get("NEW"));
    assertEquals(Long.valueOf(2), dao.countOverdueCases(session));
    assertEquals(
        Collections.singletonList(1L),
        collectCaseIds(dao.findOverdueCasesAwaitingNotification(session)));
  }

  /**
   * Generates an identity/version and reloads persisted fields after executing SQL.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesCaseAndUpdatesVersion() {
    ServiceCase item = new ServiceCase();
    item.setCaseNumber("CASE-SAVED");
    item.setTitle("Saved case");
    item.setDescription("Description");
    item.setCategory("GENERAL");
    item.setPriority("MEDIUM");
    item.setStatus("NEW");
    item.setCreatedBy("worker");
    item.setDepartment("Operations");
    item.setCreatedAt(new Date());
    item.setUpdatedAt(new Date());
    dao.save(session, item);
    dao.flush(session);
    session.clear();
    assertNotNull(item.getId());
    ServiceCase loaded = dao.findCaseById(session, item.getId());
    assertEquals("Saved case", loaded.getTitle());
    int version = loaded.getVersion();
    loaded.setTitle("Updated case");
    flushAndClear();
    assertEquals(
        Integer.valueOf(version + 1), dao.findCaseById(session, item.getId()).getVersion());
    assertNull(dao.findCaseById(session, -1));
  }

  /**
   * Compares query results using persistent identities rather than Hibernate object identity.
   *
   * @param cases the cases whose identifiers are collected
   * @return the persistent identifiers of the cases in query order
   * @throws NullPointerException if {@code cases} is {@code null}
   */
  private List<Long> collectCaseIds(List<ServiceCase> cases) {
    return cases.stream().map(ServiceCase::getId).collect(Collectors.toList());
  }
}
