package com.example.legacy.dao;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.model.ServiceCase;
import org.hibernate.Session;

import java.util.Date;
import java.util.List;

/**
 * Hibernate 3.5 native Session with named HQL queries in ServiceCase.hbm.xml. No EntityManager,
 * JPA, or annotations.
 *
 * @author Chuck Emmons
 */
public class CaseDao {
  /**
   * Returns the case with the supplied identifier or {@code null} if absent; authorization and
   * visibility checks belong to the caller.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the case with the supplied identifier or {@code null} if absent; authorization and
   *     visibility checks belong to the caller
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public ServiceCase findCaseById(Session session, long id) {
    return (ServiceCase) session.get(ServiceCase.class, id);
  }

  /**
   * Schedules a new case for persistence in the current session; the caller owns flushing and
   * committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param item the case to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, ServiceCase item) {
    session.save(item);
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

  /**
   * Returns a zero-based page ordered by latest update and descending ID to keep timestamp ties
   * stable across pages, applying optional title/number, status, and assignee filters through bound
   * parameters.
   *
   * @param session the Hibernate session used for persistence operations
   * @param search the optional title or case-number search text
   * @param status the case status code
   * @param assignee the assigned account's username
   * @param page the zero-based page index
   * @param size the number of records per page
   * @return a zero-based page ordered by latest update and descending ID to keep timestamp ties
   *     stable across pages, applying optional title/number, status, and assignee filters through
   *     bound parameters
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<ServiceCase> searchCases(
      Session session, String search, String status, String assignee, int page, int size) {
    String pattern = search == null || search.trim().isEmpty() ? null : "%" + search + "%";
    String selectedStatus = status == null || status.isEmpty() ? null : status;
    String selectedAssignee = assignee == null || assignee.isEmpty() ? null : assignee;
    return session
        .getNamedQuery("ServiceCase.search")
        .setString("q", pattern)
        .setString("status", selectedStatus)
        .setString("assignee", selectedAssignee)
        .setFirstResult(Math.max(0, page) * size)
        .setMaxResults(size)
        .list();
  }

  /**
   * Returns value/count rows grouped by status or priority for nonarchived cases; rejects all other
   * grouping properties.
   *
   * @param session the Hibernate session used for persistence operations
   * @param grouping the case property used to group the counts
   * @return value/count rows grouped by status or priority for nonarchived cases; rejects all other
   *     grouping properties
   * @throws NullPointerException if {@code grouping} or {@code session} is {@code null}
   * @throws IllegalArgumentException if the grouping has no configured reporting query
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<Object[]> countCasesByGrouping(Session session, ReportGrouping grouping) {
    switch (java.util.Objects.requireNonNull(grouping, "grouping")) {
      case STATUS:
        return session.getNamedQuery("ServiceCase.countByStatus").list();
      case PRIORITY:
        return session.getNamedQuery("ServiceCase.countByPriority").list();
      default:
        throw new IllegalArgumentException("Unsupported reporting grouping: " + grouping);
    }
  }

  /**
   * Counts nonarchived, nonclosed cases with a due timestamp earlier than the current time.
   *
   * @param session the Hibernate session used for persistence operations
   * @return the number of nonarchived, nonclosed cases whose due date has passed
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public Long countOverdueCases(Session session) {
    return (Long)
        session
            .getNamedQuery("ServiceCase.countOverdue")
            .setString("closedStatus", CaseStatus.CLOSED.name())
            .setTimestamp("now", new Date())
            .uniqueResult();
  }

  /**
   * Returns up to 300 nonarchived, nonclosed overdue cases whose overdue-notification flag is still
   * {@code false}.
   *
   * @param session the Hibernate session used for persistence operations
   * @return up to 300 nonarchived, nonclosed overdue cases whose overdue-notification flag is still
   *     {@code false}
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<ServiceCase> findOverdueCasesAwaitingNotification(Session session) {
    return session
        .getNamedQuery("ServiceCase.findOverdueAwaitingNotification")
        .setString("closedStatus", CaseStatus.CLOSED.name())
        .setTimestamp("now", new Date())
        .setMaxResults(300)
        .list();
  }
}
