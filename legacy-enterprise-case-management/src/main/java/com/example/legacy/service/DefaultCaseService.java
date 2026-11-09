package com.example.legacy.service;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.CaseDocumentDao;
import com.example.legacy.dao.CaseNoteDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.domain.CaseStatus;
import com.example.legacy.model.Account;
import com.example.legacy.model.CaseDocument;
import com.example.legacy.model.CaseNote;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.dto.AddCaseCommentDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseNoteDto;
import com.example.legacy.service.dto.CaseSearchCriteriaDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.dto.TransitionCaseDto;
import com.example.legacy.service.dto.UpdateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.service.exception.RecordNotFoundException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.CaseNoteMapper;
import com.example.legacy.service.mapper.DocumentMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.AddCaseCommentDtoValidator;
import com.example.legacy.service.validation.AssignCaseDtoValidator;
import com.example.legacy.service.validation.CaseSearchCriteriaDtoValidator;
import com.example.legacy.service.validation.CreateCaseDtoValidator;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.service.validation.TransitionCaseDtoValidator;
import com.example.legacy.service.validation.UpdateCaseDtoValidator;
import com.example.legacy.util.CaseDefaults;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import com.example.legacy.util.ValidationLimits;
import com.example.legacy.util.WorkflowUtil;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Coordinates case operations, policy checks, and atomic audit/notification side effects.
 *
 * @author Chuck Emmons
 */
public class DefaultCaseService implements CaseService {
  private final CaseSearchCriteriaDtoValidator caseSearchCriteriaValidator =
      new CaseSearchCriteriaDtoValidator();
  private final RecordReferenceDtoValidator recordReferenceValidator =
      new RecordReferenceDtoValidator();
  private final CreateCaseDtoValidator createCaseValidator = new CreateCaseDtoValidator();
  private final UpdateCaseDtoValidator updateCaseValidator = new UpdateCaseDtoValidator();
  private final AssignCaseDtoValidator assignCaseValidator = new AssignCaseDtoValidator();
  private final TransitionCaseDtoValidator transitionCaseValidator =
      new TransitionCaseDtoValidator();
  private final AddCaseCommentDtoValidator addCaseCommentValidator =
      new AddCaseCommentDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final CaseDao caseDao;
  private final CaseNoteDao caseNoteDao;
  private final CaseDocumentDao caseDocumentDao;
  private final CaseMapper caseMapper;
  private final CaseNoteMapper caseNoteMapper;
  private final DocumentMapper documentMapper;
  private final AccountMapper accountMapper;
  private final AccountAccessService accountAccessService;
  private final CasePolicyService casePolicyService;
  private final AuditService auditService;
  private final NotificationService notificationService;

  /** Creates lazy production collaborators without opening a database connection. */
  public DefaultCaseService() {
    this((SessionFactory) null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public DefaultCaseService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new CaseDao(),
        new CaseNoteDao(),
        new CaseDocumentDao(),
        new CaseMapper(),
        new CaseNoteMapper(),
        new DocumentMapper(),
        new AccountMapper(),
        new AccountAccessService(sessionFactory),
        new CasePolicyService(),
        new AuditService(sessionFactory),
        new NotificationService(sessionFactory));
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param caseDao the case DAO collaborator
   * @param caseNoteDao the case note DAO collaborator
   * @param caseDocumentDao the case document DAO collaborator
   * @param caseMapper the case mapper collaborator
   * @param caseNoteMapper the case note mapper collaborator
   * @param documentMapper the document mapper collaborator
   * @param accountMapper the account mapper collaborator
   * @param accountAccessService the account access service collaborator
   * @param casePolicyService the case policy service collaborator
   * @param auditService the audit service collaborator
   * @param notificationService the notification service collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code caseDao} or {@code
   *     caseNoteDao} or {@code caseDocumentDao} or {@code caseMapper} or {@code caseNoteMapper} or
   *     {@code documentMapper} or {@code accountMapper} or {@code accountAccessService} or {@code
   *     casePolicyService} or {@code auditService} or {@code notificationService} is {@code null}
   */
  public DefaultCaseService(
      HibernateTransactionTemplate transactionTemplate,
      CaseDao caseDao,
      CaseNoteDao caseNoteDao,
      CaseDocumentDao caseDocumentDao,
      CaseMapper caseMapper,
      CaseNoteMapper caseNoteMapper,
      DocumentMapper documentMapper,
      AccountMapper accountMapper,
      AccountAccessService accountAccessService,
      CasePolicyService casePolicyService,
      AuditService auditService,
      NotificationService notificationService) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.caseDao = Objects.requireNonNull(caseDao, "caseDao");
    this.caseNoteDao = Objects.requireNonNull(caseNoteDao, "caseNoteDao");
    this.caseDocumentDao = Objects.requireNonNull(caseDocumentDao, "caseDocumentDao");
    this.caseMapper = Objects.requireNonNull(caseMapper, "caseMapper");
    this.caseNoteMapper = Objects.requireNonNull(caseNoteMapper, "caseNoteMapper");
    this.documentMapper = Objects.requireNonNull(documentMapper, "documentMapper");
    this.accountMapper = Objects.requireNonNull(accountMapper, "accountMapper");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.casePolicyService = Objects.requireNonNull(casePolicyService, "casePolicyService");
    this.auditService = Objects.requireNonNull(auditService, "auditService");
    this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
  }

  /**
   * Returns a page of nonarchived cases matching optional search, status, and assignee filters,
   * ordered by latest update. Page indexes are zero-based.
   *
   * @param actor the authenticated actor's username
   * @param request the case filters and requested page
   * @return a page of nonarchived cases matching optional search, status, and assignee filters,
   *     ordered by latest update
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public List<CaseDto> searchCases(final String actor, final CaseSearchCriteriaDto request) {
    final CaseSearchCriteriaDto validated = caseSearchCriteriaValidator.validate(request);

    return transactionTemplate.execute(session -> {
      accountAccessService.requireActiveAccount(session, actor);

      return caseMapper.mapToDtos(
          caseDao.searchCases(
              session,
              validated.getQuery(),
              validated.getStatus(),
              validated.getAssignee(),
              validated.getPage(),
              ValidationLimits.CASE_PAGE_SIZE));
    });
  }

  /**
   * Loads detached case data and computes capabilities without depending on servlet requests.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @return the case snapshot and its associated notes, documents, and permitted transitions
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public CaseDetails getCaseDetails(final String actor, final RecordReferenceDto request) {
    final RecordReferenceDto validated = recordReferenceValidator.validate(request);
    final long id = validated.getId();

    return transactionTemplate.execute(session -> {
      Account account = accountAccessService.requireActiveAccount(session, actor);
      ServiceCase item = requireCaseById(session, id);

      return CaseDetails.builder()
          .item(caseMapper.mapToDto(item))
          .notes(caseNoteMapper.mapToDtos(listCaseNotes(session, id)))
          .documents(documentMapper.mapToMetadataDtos(listCaseDocuments(session, id)))
          .users(accountMapper.mapToDtos(accountAccessService.listAllAccounts(session)))
          .nextStates(casePolicyService.getAllowedNextStatuses(account, item))
          .mayEdit(casePolicyService.canEditCase(account, item))
          .mayComment(WorkflowUtil.canWorkOnCases(account.getRole()))
          .build();
    });
  }

  /**
   * Validates an active working user and case fields, creates a NEW case with an ID-based case
   * number, and records its audit event atomically.
   *
   * @param actor the authenticated actor's username
   * @param request the initial case details
   * @return the newly created case snapshot
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public CaseDto createCase(final String actor, final CreateCaseDto request) {
    final CreateCaseDto validated = createCaseValidator.validate(request);

    return transactionTemplate.execute(session -> {
      Account activeAccount = accountAccessService.requireActiveAccount(session, actor);
      accountAccessService.requireCaseWorkPermission(activeAccount);

      CaseDto input =
          CaseDto.builder()
              .title(validated.getTitle())
              .description(validated.getDescription())
              .category(validated.getCategory())
              .priority(validated.getPriority())
              .department(validated.getDepartment())
              .dueAt(validated.getDueAt())
              .build();

      ServiceCase entity = caseMapper.mapToEntity(input);
      entity.setCaseNumber(
          CaseDefaults.TEMPORARY_NUMBER_PREFIX + UUID.randomUUID().toString().replace("-", ""));
      entity.setCreatedBy(actor);
      entity.setStatus(CaseStatus.NEW.name());
      entity.setCreatedAt(new Date());
      entity.setUpdatedAt(new Date());
      entity.setArchived(false);
      entity.setOverdueNoticeSent(false);

      caseDao.save(session, entity);
      caseDao.flush(session);

      entity.setCaseNumber(String.format(CaseDefaults.NUMBER_FORMAT, entity.getId()));

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          entity.getId(),
          AuditAction.CREATE,
          actor,
          MessagesUtil.formatMessage(MessageKeys.AUDIT_CASE_OPENED, entity.getCaseNumber()));

      caseDao.flush(session);

      return caseMapper.mapToDto(entity);
    });
  }

  /**
   * Updates an open case after checking working permissions, worker ownership, and the submitted
   * optimistic-lock version; records an audit event atomically.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference, expected version, and editable fields
   * @return the case snapshot after applying the requested changes
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public CaseDto updateCase(final String actor, final UpdateCaseDto request) {
    final UpdateCaseDto validated = updateCaseValidator.validate(request);
    final long id = validated.getId();

    return transactionTemplate.execute(session -> {
      Account activeAccount = accountAccessService.requireActiveAccount(session, actor);
      ServiceCase entity = requireCaseById(session, id);

      casePolicyService.requireCaseEditPermission(activeAccount, entity);

      if (!validated.getVersion().equals(entity.getVersion())) {
        throw new ConcurrentUpdateException(
            MessageKeys.BUSINESS_ANOTHER_USER_UPDATED_THIS_CASE_REFRESH_AND_RETRY);
      }

      CaseDto input =
          CaseDto.builder()
              .title(validated.getTitle())
              .description(validated.getDescription())
              .priority(validated.getPriority())
              .dueAt(validated.getDueAt())
              .build();

      caseMapper.updateEntity(input, entity);
      entity.setUpdatedAt(new Date());

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          id,
          AuditAction.UPDATE,
          actor,
          MessagesUtil.formatMessage(MessageKeys.AUDIT_CASE_DETAILS_UPDATED));

      caseDao.flush(session);

      return caseMapper.mapToDto(entity);
    });
  }

  /**
   * Requires supervisor privileges to assign an open case to an active working user, updating its
   * timestamp and creating an audit event and recipient notice.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and assignee
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if the request fields are invalid or the case is closed
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void assignCase(final String actor, final AssignCaseDto request) {
    final AssignCaseDto validated = assignCaseValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireSupervisorPermission(
          accountAccessService.requireActiveAccount(session, actor));

      Account target = accountAccessService.requireActiveAccount(session, validated.getAssignee());
      accountAccessService.requireCaseWorkPermission(target);

      ServiceCase serviceCase = requireCaseById(session, id);

      if (CaseStatus.CLOSED.name().equals(serviceCase.getStatus())) {
        throw new BusinessValidationException(
            MessageKeys.BUSINESS_CLOSED_CASES_CANNOT_BE_REASSIGNED);
      }

      String previous = serviceCase.getAssignedTo();

      CaseDto data = caseMapper.mapToDto(serviceCase);
      data =
          CaseDto.builder(data).assignedTo(validated.getAssignee()).updatedAt(new Date()).build();

      caseMapper.updateState(data, serviceCase);

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          id,
          AuditAction.ASSIGN,
          actor,
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_CASE_ASSIGNED, previous, validated.getAssignee()));

      notificationService.createNotification(
          session,
          validated.getAssignee(),
          id,
          MessagesUtil.formatMessage(MessageKeys.NOTICE_CASE_ASSIGNED, serviceCase.getCaseNumber()),
          serviceCase.getTitle());
    });
  }

  /**
   * Checks role permissions, assigned-worker ownership, and the creator self-approval restriction
   * before changing status, recording closure time, audit history, and notices atomically.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and destination status
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if the request fields are invalid or the case creator
   *     attempts self-approval
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code actor} or {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void transitionCase(final String actor, final TransitionCaseDto request) {
    final TransitionCaseDto validated = transitionCaseValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      Account account = accountAccessService.requireActiveAccount(session, actor);
      ServiceCase serviceCase = requireCaseById(session, id);

      casePolicyService.requireCaseTransitionPermission(
          account, serviceCase, validated.getDestination());

      final String previousStatus = serviceCase.getStatus();

      CaseDto data = caseMapper.mapToDto(serviceCase);
      data = CaseDto.builder(data).status(validated.getDestination()).updatedAt(new Date()).build();

      if (CaseStatus.CLOSED.name().equals(validated.getDestination())) {
        data = CaseDto.builder(data).closedAt(new Date()).build();
      }

      caseMapper.updateState(data, serviceCase);

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          id,
          AuditAction.TRANSITION,
          actor,
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_STATUS_CHANGED, previousStatus, validated.getDestination()));

      notificationService.createNotification(
          session,
          serviceCase.getCreatedBy(),
          id,
          MessagesUtil.formatMessage(MessageKeys.NOTICE_CASE_STATUS, serviceCase.getCaseNumber()),
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_STATUS_CHANGED, previousStatus, validated.getDestination()));

      if (!actor.equals(serviceCase.getAssignedTo())) {
        notificationService.createNotification(
            session,
            serviceCase.getAssignedTo(),
            id,
            MessagesUtil.formatMessage(MessageKeys.NOTICE_CASE_STATUS, serviceCase.getCaseNumber()),
            MessagesUtil.formatMessage(
                MessageKeys.AUDIT_STATUS_CHANGED, previousStatus, validated.getDestination()));
      }
    });
  }

  /**
   * Allows an active working user to add a validated note to a visible case, recording an audit
   * event and notifying a different assignee.
   *
   * @param actor the authenticated actor's username
   * @param request the case reference and comment text
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code actor} or {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void addCaseComment(final String actor, final AddCaseCommentDto request) {
    final AddCaseCommentDto validated = addCaseCommentValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireCaseWorkPermission(
          accountAccessService.requireActiveAccount(session, actor));

      final ServiceCase serviceCase = requireCaseById(session, id);

      CaseNoteDto input = CaseNoteDto.builder().body(validated.getBody()).build();

      CaseNote note = caseNoteMapper.mapToEntity(input);
      note.setCaseId(id);
      note.setAuthor(actor);
      note.setCreatedAt(new Date());

      caseNoteDao.save(session, note);

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          id,
          AuditAction.COMMENT,
          actor,
          MessagesUtil.formatMessage(MessageKeys.AUDIT_COMMENT_ADDED));

      if (!actor.equals(serviceCase.getAssignedTo())) {
        notificationService.createNotification(
            session,
            serviceCase.getAssignedTo(),
            id,
            MessagesUtil.formatMessage(
                MessageKeys.NOTICE_CASE_COMMENT, serviceCase.getCaseNumber()),
            MessagesUtil.formatMessage(MessageKeys.NOTICE_COMMENT_BODY, actor));
      }
    });
  }

  /**
   * Requires an active administrator to soft-delete a case and record the change in the same
   * transaction.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws ConcurrentUpdateException if submitted data is stale or Hibernate detects an
   *     optimistic-lock conflict
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @Override
  public void archiveCase(final String actor, final RecordReferenceDto request) {
    final RecordReferenceDto validated = recordReferenceValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireAdministratorPermission(
          accountAccessService.requireActiveAccount(session, actor));

      ServiceCase serviceCase = requireCaseById(session, id);
      CaseDto data = caseMapper.mapToDto(serviceCase);
      data = CaseDto.builder(data).archived(true).updatedAt(new Date()).build();

      caseMapper.updateState(data, serviceCase);

      auditService.recordAuditEvent(
          session,
          AuditEntity.CASE,
          id,
          AuditAction.ARCHIVE,
          actor,
          MessagesUtil.formatMessage(MessageKeys.AUDIT_CASE_ARCHIVED_SOFT_DELETE));
    });
  }

  /**
   * Lists a case's discussion notes newest first using the supplied session.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the notes belonging to the case, newest first
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  List<CaseNote> listCaseNotes(Session session, long id) {
    return caseNoteDao.findNotesByCaseId(session, id);
  }

  /**
   * Lists a case's attachments newest first using the supplied session.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return the documents belonging to the case, newest first
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  List<CaseDocument> listCaseDocuments(Session session, long id) {
    return caseDocumentDao.findDocumentsByCaseId(session, id);
  }

  /**
   * Returns an existing, nonarchived case from the supplied session or throws
   * BusinessValidationException.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @return an existing, nonarchived case from the supplied session or throws
   *     BusinessValidationException
   * @throws RecordNotFoundException if the requested case does not exist or is archived
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private ServiceCase requireCaseById(Session session, long id) {
    ServiceCase entity = caseDao.findCaseById(session, id);

    if (entity == null || entity.isArchived()) {
      throw new RecordNotFoundException(MessageKeys.BUSINESS_CASE_NOT_FOUND);
    }

    return entity;
  }
}
