package com.example.legacy.integration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.CaseNoteDao;
import com.example.legacy.model.CaseNote;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

/**
 * Verifies note ordering, case scoping, and foreign-key enforcement with a real H2 session.
 *
 * @author Chuck Emmons
 */
public class CaseNoteDaoIntegrationTest extends H2DaoTestSupport {
  private final CaseNoteDao dao = new CaseNoteDao();

  /**
   * Returns only the selected case's notes in descending creation order.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void scopesAndOrdersNotes() {
    assertEquals(
        Arrays.asList(11L, 10L),
        dao.findNotesByCaseId(session, 1).stream()
            .map(CaseNote::getId)
            .collect(Collectors.toList()));
    assertTrue(dao.findNotesByCaseId(session, -1).isEmpty());
  }

  /**
   * Persists a note and observes its generated identity after SQL execution.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesNote() {
    CaseNote note = createNote(1);
    dao.save(session, note);
    flushAndClear();
    assertNotNull(note.getId());
    assertEquals(3, dao.findNotesByCaseId(session, 1).size());
  }

  /**
   * Rejects orphan references using the database's actual foreign key.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rejectsOrphanNotes() {
    dao.save(session, createNote(-1));
    assertThrows(ConstraintViolationException.class, () -> session.flush());
  }

  /**
   * Creates complete note input without bypassing the production entity mapping.
   *
   * @param caseId the case identifier
   * @return the case note
   */
  private CaseNote createNote(long caseId) {
    CaseNote note = new CaseNote();
    note.setCaseId(caseId);
    note.setAuthor("worker");
    note.setBody("Saved note");
    note.setCreatedAt(new Date());
    return note;
  }
}
