package com.example.legacy.unit.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.model.CaseDocument;
import java.util.Collections;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Test;

/**
 * Behavioral contracts for CaseDocumentDao.
 *
 * @author Chuck Emmons
 */
public class CaseDocumentDaoTest {

  /**
   * Scopes attachment lists to the case and delegates identity lookup.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void scopesAttachmentListsToTheCaseAndDelegatesIdentityLookup() {
    Session session = mock(Session.class);
    Query query = mock(Query.class, RETURNS_SELF);
    when(session.getNamedQuery(anyString())).thenReturn(query);
    CaseDocument document = new CaseDocument();
    when(query.list()).thenReturn(Collections.singletonList(document));
    when(session.get(CaseDocument.class, 9L)).thenReturn(document);
    CaseDocumentDao dao = new CaseDocumentDao();
    assertEquals(Collections.singletonList(document), dao.findDocumentsByCaseId(session, 7));
    verify(query).setLong("id", 7);
    assertSame(document, dao.findDocumentById(session, 9));
    dao.save(session, document);
    verify(session).save(document);
  }
}
