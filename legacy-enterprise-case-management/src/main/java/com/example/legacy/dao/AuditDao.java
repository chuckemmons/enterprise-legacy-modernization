package com.example.legacy.dao;

import com.example.legacy.model.AuditEvent;
import java.util.List;
import org.hibernate.Session;

/**
 * Persists audit events and pages recent history using a caller-owned Hibernate session.
 *
 * @author Chuck Emmons
 */
public class AuditDao {
  /**
   * Schedules a new audit event for persistence in the current session; the caller owns flushing
   * and committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param event the audit event to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, AuditEvent event) {
    session.save(event);
  }

  /**
   * Returns up to 50 audit events for a zero-based page, ordered newest first and clamping negative
   * page indexes to zero.
   *
   * @param session the Hibernate session used for persistence operations
   * @param page the zero-based page index
   * @return up to 50 audit events for a zero-based page, ordered newest first and clamping negative
   *     page indexes to zero
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<AuditEvent> findRecentAuditEvents(Session session, int page) {
    return session
        .getNamedQuery("AuditEvent.findRecent")
        .setFirstResult(
            Math.max(0, page) * com.example.legacy.util.ValidationLimits.AUDIT_PAGE_SIZE)
        .setMaxResults(com.example.legacy.util.ValidationLimits.AUDIT_PAGE_SIZE)
        .list();
  }
}
