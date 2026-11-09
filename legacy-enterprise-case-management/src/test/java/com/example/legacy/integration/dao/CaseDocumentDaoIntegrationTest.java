package com.example.legacy.integration.dao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.model.CaseDocument;
import java.util.Date;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

/**
 * Verifies the production document mapping, binary payload persistence, and case foreign key in H2.
 *
 * @author Chuck Emmons
 */
public class CaseDocumentDaoIntegrationTest extends H2DaoTestSupport {
  private final CaseDocumentDao dao = new CaseDocumentDao();

  /**
   * Loads seeded attachment metadata/bytes and scopes attachment lists by parent case.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void readsSeededAttachments() {
    assertEquals("seed.txt", dao.findDocumentById(session, 20).getOriginalName());
    assertArrayEquals(new byte[] {1, 2, 3}, dao.findDocumentById(session, 20).getPayload());
    assertEquals(1, dao.findDocumentsByCaseId(session, 1).size());
    assertTrue(dao.findDocumentsByCaseId(session, -1).isEmpty());
    assertNull(dao.findDocumentById(session, -1));
  }

  /**
   * Flushes and reloads a binary payload without relying on the first-level Hibernate cache.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void savesBinaryPayload() {
    CaseDocument document = createDocument(1);
    dao.save(session, document);
    flushAndClear();
    CaseDocument loaded = dao.findDocumentById(session, document.getId());
    assertArrayEquals(new byte[] {4, 5, 6, 7}, loaded.getPayload());
    assertEquals(4, loaded.getContentSize());
    assertEquals(2, dao.findDocumentsByCaseId(session, 1).size());
  }

  /**
   * Enforces parent-case integrity in the real test database.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Test
  public void rejectsOrphanAttachments() {
    dao.save(session, createDocument(-1));
    assertThrows(ConstraintViolationException.class, () -> session.flush());
  }

  /**
   * Creates complete attachment input for persistence checks.
   *
   * @param caseId the case identifier
   * @return the case document
   */
  private CaseDocument createDocument(long caseId) {
    CaseDocument document = new CaseDocument();
    document.setCaseId(caseId);
    document.setOriginalName("saved.bin");
    document.setContentType("application/octet-stream");
    document.setContentSize(4);
    document.setPayload(new byte[] {4, 5, 6, 7});
    document.setUploadedBy("worker");
    document.setCreatedAt(new Date());
    return document;
  }
}
