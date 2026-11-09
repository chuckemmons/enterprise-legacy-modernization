package com.example.legacy.dao;

import com.example.legacy.model.CaseNotice;
import java.util.List;
import org.hibernate.Session;

/**
 * Persists notifications and queries recipient history and pending email delivery using a
 * caller-owned session.
 *
 * @author Chuck Emmons
 */
public class NoticeDao {
  /**
   * Schedules a new notification for persistence in the current session; the caller owns flushing
   * and committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param notice the notification to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, CaseNotice notice) {
    session.save(notice);
  }

  /**
   * Returns the notification with the supplied identifier or {@code null} if absent; authorization
   * and visibility checks belong to the caller.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the notification with the supplied identifier or {@code null} if absent; authorization
   *     and visibility checks belong to the caller
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public CaseNotice findNotificationById(Session session, long id) {
    return (CaseNotice) session.get(CaseNotice.class, id);
  }

  /**
   * Returns the latest 100 notifications for the exact recipient username, including read notices.
   *
   * @param session the Hibernate session used for persistence operations
   * @param user the user's username
   * @return the latest 100 notifications for the exact recipient username, including read notices
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<CaseNotice> findNotificationsByUsername(Session session, String user) {
    return session
        .getNamedQuery("CaseNotice.findByRecipient")
        .setString("user", user)
        .setMaxResults(100)
        .list();
  }

  /**
   * Returns up to 50 notification identifiers awaiting email processing, oldest first.
   *
   * @param session the Hibernate session used for persistence operations
   * @return up to 50 notification identifiers awaiting email processing, oldest first
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<Long> findPendingEmailNotificationIds(Session session) {
    return session.getNamedQuery("CaseNotice.findPendingEmailIds").setMaxResults(50).list();
  }
}
