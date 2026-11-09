package com.example.legacy.unit.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.model.Account;
import com.example.legacy.model.CaseDocument;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.AuditService;
import com.example.legacy.service.CasePolicyService;
import com.example.legacy.service.DocumentService;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.exception.RecordNotFoundException;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.unit.support.ServiceTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavioral contracts for DocumentService.
 *
 * @author Chuck Emmons
 */
public class DocumentServiceTest extends ServiceTestSupport {

  private CaseDao cases;
  private CaseDocumentDao documents;
  private AccountAccessService access;
  private CasePolicyService policy;
  private AuditService audit;
  private DocumentService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    cases = mock(CaseDao.class);
    documents = mock(CaseDocumentDao.class);
    access = mock(AccountAccessService.class);
    policy = mock(CasePolicyService.class);
    audit = mock(AuditService.class);
    service =
        new DocumentService(
            template, documents, new DocumentMapper(), access, cases, policy, audit);
  }

  /**
   * Uploads validated bytes with ownership and audit in one transaction.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void uploadsValidatedBytesWithOwnershipAndAuditInOneTransaction() {
    Account actor = account("worker", "WORKER");
    ServiceCase item = new ServiceCase();
    when(access.requireActiveAccount(session, "worker")).thenReturn(actor);
    when(cases.findCaseById(session, 7)).thenReturn(item);
    DocumentDto input =
        DocumentDto.builder()
            .originalName("note.txt")
            .contentType("text/plain")
            .payload(new byte[] {1, 2})
            .build();
    service.uploadDocument(
        "worker", UploadDocumentDto.builder().caseId(7).declaredSize(2).document(input).build());
    ArgumentCaptor<CaseDocument> saved = ArgumentCaptor.forClass(CaseDocument.class);
    verify(documents).save(eq(session), saved.capture());
    assertEquals(Long.valueOf(7), saved.getValue().getCaseId());
    assertEquals("worker", saved.getValue().getUploadedBy());
    assertArrayEquals(new byte[] {1, 2}, saved.getValue().getPayload());
    assertNotNull(saved.getValue().getCreatedAt());
    verify(policy).requireCaseEditPermission(actor, item);
    verify(audit)
        .recordAuditEvent(
            eq(session),
            eq(AuditEntity.CASE),
            eq(7L),
            eq(AuditAction.UPLOAD),
            eq("worker"),
            anyString());
    verify(transaction).commit();
  }

  /**
   * Downloads only documents whose parent case is visible.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void downloadsOnlyDocumentsWhoseParentCaseIsVisible() {
    CaseDocument document = new CaseDocument();
    document.setCaseId(7L);
    document.setOriginalName("note.txt");
    document.setPayload(new byte[] {3});
    document.setContentSize(1);
    when(documents.findDocumentById(session, 9)).thenReturn(document);
    ServiceCase item = new ServiceCase();
    when(cases.findCaseById(session, 7)).thenReturn(item);
    assertArrayEquals(
        new byte[] {3},
        service
            .downloadDocument("worker", RecordReferenceDto.builder().id(9).build())
            .getPayload());
    item.setArchived(true);
    assertThrows(
        RecordNotFoundException.class,
        () -> service.downloadDocument("worker", RecordReferenceDto.builder().id(9).build()));
    when(cases.findCaseById(session, 7)).thenReturn(null);
    assertThrows(
        RecordNotFoundException.class,
        () -> service.downloadDocument("worker", RecordReferenceDto.builder().id(9).build()));
    assertThrows(
        RecordNotFoundException.class,
        () -> service.downloadDocument("worker", RecordReferenceDto.builder().id(99).build()));
  }

  /**
   * Rolls back an unauthorized upload without saving bytes.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rollsBackAnUnauthorizedUploadWithoutSavingBytes() {
    ServiceCase item = new ServiceCase();
    when(cases.findCaseById(session, 7)).thenReturn(item);
    Account actor = account("worker", "WORKER");
    when(access.requireActiveAccount(session, "worker")).thenReturn(actor);
    doThrow(new AccessDeniedException("denied"))
        .when(policy)
        .requireCaseEditPermission(actor, item);
    assertThrows(
        AccessDeniedException.class,
        () ->
            service.uploadDocument(
                "worker",
                UploadDocumentDto.builder()
                    .caseId(7)
                    .declaredSize(1)
                    .document(
                        DocumentDto.builder()
                            .originalName("note.txt")
                            .payload(new byte[] {1})
                            .build())
                    .build()));
    verifyNoInteractions(documents, audit);
    verify(transaction).rollback();
  }
}
