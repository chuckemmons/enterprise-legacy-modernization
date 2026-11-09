package com.example.legacy.dao;

import com.example.legacy.model.CaseNote;
import java.util.List;
import org.hibernate.Session;

/**
 * Persists and lists case discussion notes using a caller-owned Hibernate session.
 *
 * @author Chuck Emmons
 */
public class CaseNoteDao {
  /**
   * Schedules a new note for persistence in the current session; the caller owns flushing and
   * committing.
   *
   * @param session the Hibernate session used for persistence operations
   * @param item the note to persist
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void save(Session session, CaseNote item) {
    session.save(item);
  }

  /**
   * Returns all notes for the supplied case identifier, newest first; the caller checks case
   * visibility.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return all notes for the supplied case identifier, newest first; the caller checks case
   *     visibility
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<CaseNote> findNotesByCaseId(Session session, long id) {
    return session.getNamedQuery("CaseNote.findByCaseId").setLong("id", id).list();
  }
}
