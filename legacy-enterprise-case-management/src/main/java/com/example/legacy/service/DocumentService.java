package com.example.legacy.service;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.model.Account;
import com.example.legacy.model.CaseDocument;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.dto.DocumentDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.UploadDocumentDto;
import com.example.legacy.service.exception.RecordNotFoundException;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.service.validation.UploadDocumentDtoValidator;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.Date;
import java.util.Objects;

/**
 * Validates attachment commands and coordinates authorized document persistence with atomic audit
 * writes.
 *
 * @author Chuck Emmons
 */
public class DocumentService {
  private final UploadDocumentDtoValidator uploadDocumentValidator =
      new UploadDocumentDtoValidator();
  private final RecordReferenceDtoValidator recordReferenceValidator =
      new RecordReferenceDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final CaseDocumentDao caseDocumentDao;
  private final DocumentMapper documentMapper;
  private final AccountAccessService accountAccessService;
  private final CaseDao caseDao;
  private final CasePolicyService casePolicyService;
  private final AuditService auditService;

  /** Creates lazy production collaborators without opening a database connection. */
  public DocumentService() {
    this(null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public DocumentService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new CaseDocumentDao(),
        new DocumentMapper(),
        new AccountAccessService(sessionFactory),
        new CaseDao(),
        new CasePolicyService(),
        new AuditService(sessionFactory));
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param caseDocumentDao the case document DAO collaborator
   * @param documentMapper the document mapper collaborator
   * @param accountAccessService the account access service collaborator
   * @param caseDao the case DAO collaborator
   * @param casePolicyService the case policy service collaborator
   * @param auditService the audit service collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code caseDocumentDao} or
   *     {@code documentMapper} or {@code accountAccessService} or {@code caseDao} or {@code
   *     casePolicyService} or {@code auditService} is {@code null}
   */
  public DocumentService(
      HibernateTransactionTemplate transactionTemplate,
      CaseDocumentDao caseDocumentDao,
      DocumentMapper documentMapper,
      AccountAccessService accountAccessService,
      CaseDao caseDao,
      CasePolicyService casePolicyService,
      AuditService auditService) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.caseDocumentDao = Objects.requireNonNull(caseDocumentDao, "caseDocumentDao");
    this.documentMapper = Objects.requireNonNull(documentMapper, "documentMapper");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.caseDao = Objects.requireNonNull(caseDao, "caseDao");
    this.casePolicyService = Objects.requireNonNull(casePolicyService, "casePolicyService");
    this.auditService = Objects.requireNonNull(auditService, "auditService");
  }

  /**
   * Validates attachment content, then saves it with an audit event after checking case-edit
   * permissions.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference, attachment metadata, and file contents
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void uploadDocument(final String actor, final UploadDocumentDto request) {
    final UploadDocumentDto validated = uploadDocumentValidator.validate(request);

    final long caseId = validated.getCaseId();
    final DocumentDto input = validated.getDocument();

    transactionTemplate.executeWithoutResult(session -> {
      Account account = accountAccessService.requireActiveAccount(session, actor);
      ServiceCase item = requireVisibleCase(session, caseId);
      casePolicyService.requireCaseEditPermission(account, item);

      CaseDocument document = documentMapper.mapToEntity(input);
      document.setCaseId(caseId);
      document.setUploadedBy(actor);
      document.setCreatedAt(new Date());

      caseDocumentDao.save(session, document);

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          caseId,
          AuditAction.UPLOAD,
          actor,
          MessagesUtil.formatMessage(MessageKeys.AUDIT_DOCUMENT_UPLOADED, input.getOriginalName()));
    });
  }

  /**
   * Returns an attachment only for an active actor and a visible, nonarchived parent case.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @return an attachment only for an active actor and a visible, nonarchived parent case
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RecordNotFoundException if the document or its parent case is missing or archived
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public DocumentDto downloadDocument(final String actor, final RecordReferenceDto request) {
    final RecordReferenceDto validated = recordReferenceValidator.validate(request);

    return transactionTemplate.execute(session -> {
      accountAccessService.requireActiveAccount(session, actor);

      CaseDocument document = caseDocumentDao.findDocumentById(session, validated.getId());

      if (document == null) {
        throw new RecordNotFoundException(MessageKeys.BUSINESS_DOCUMENT_NOT_FOUND);
      }

      requireVisibleCase(session, document.getCaseId());

      return documentMapper.mapToDto(document);
    });
  }

  /**
   * Requires a visible parent case within the document operation's existing transaction.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the existing, nonarchived case
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private ServiceCase requireVisibleCase(Session session, long id) {
    ServiceCase item = caseDao.findCaseById(session, id);

    if (item == null || item.isArchived()) {
      throw new RecordNotFoundException(MessageKeys.BUSINESS_CASE_NOT_FOUND);
    }

    return item;
  }
}
