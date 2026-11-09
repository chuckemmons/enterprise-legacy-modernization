package com.example.legacy.dao;

import com.example.legacy.model.CaseDocument;
import java.util.List;
import org.hibernate.Session;

/**
 * Persists and loads case attachment metadata and payloads using a caller-owned Hibernate session.
 *
 * @author Chuck Emmons
 */
public class CaseDocumentDao {
  /**
   * Schedules a new attachment for persistence in the current session; the caller owns flushing and
   * committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param item the attachment to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, CaseDocument item) {
    session.save(item);
  }

  /**
   * Returns the attachment with the supplied identifier or {@code null} if absent; authorization
   * and visibility checks belong to the caller.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the attachment with the supplied identifier or {@code null} if absent; authorization
   *     and visibility checks belong to the caller
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public CaseDocument findDocumentById(Session session, long id) {
    return (CaseDocument) session.get(CaseDocument.class, id);
  }

  /**
   * Returns all attachments for the supplied case identifier, newest first; the caller checks case
   * visibility.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return all attachments for the supplied case identifier, newest first; the caller checks case
   *     visibility
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<CaseDocument> findDocumentsByCaseId(Session session, long id) {
    return session.getNamedQuery("CaseDocument.findByCaseId").setLong("id", id).list();
  }
}
