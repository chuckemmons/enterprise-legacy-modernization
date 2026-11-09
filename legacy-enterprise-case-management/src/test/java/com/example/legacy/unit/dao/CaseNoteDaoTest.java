package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseNoteDao;
import com.example.legacy.model.CaseNote;
import java.util.Collections;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for CaseNoteDao.
 *
 * @author Chuck Emmons
 */
public class CaseNoteDaoTest {

  /**
   * Scopes discussion to the requested case.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void scopesDiscussionToTheRequestedCase() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    CaseNote note = new CaseNote();
    when(query.list()).thenReturn(Collections.singletonList(note));
    CaseNoteDao dao = new CaseNoteDao();
    assertEquals(Collections.singletonList(note), dao.findNotesByCaseId(session, 7));
    verify(query).setLong("id", 7);
    dao.save(session, note);
    verify(session).save(note);
  }
}
