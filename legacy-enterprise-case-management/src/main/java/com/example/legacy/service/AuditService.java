package com.example.legacy.service;

import com.example.legacy.dao.AuditDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.mapper.AuditEventMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.AuditEventDtoValidator;
import com.example.legacy.service.validation.PageRequestDtoValidator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Records business audit events on the supplied session and performs authorized audit queries.
 *
 * @author Chuck Emmons
 */
public class AuditService {
  private final AuditEventDtoValidator auditEventValidator = new AuditEventDtoValidator();
  private final PageRequestDtoValidator pageRequestValidator = new PageRequestDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountAccessService accountAccessService;
  private final AuditDao auditDao;
  private final AuditEventMapper auditEventMapper;

  /** Creates lazy production collaborators without opening a database connection. */
  public AuditService() {
    this((SessionFactory) null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public AuditService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountAccessService(sessionFactory),
        new AuditDao(),
        new AuditEventMapper());
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountAccessService the account access service collaborator
   * @param auditDao the audit DAO collaborator
   * @param auditEventMapper the audit event mapper collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountAccessService} or
   *     {@code auditDao} or {@code auditEventMapper} is {@code null}
   */
  public AuditService(
      HibernateTransactionTemplate transactionTemplate,
      AccountAccessService accountAccessService,
      AuditDao auditDao,
      AuditEventMapper auditEventMapper) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.auditDao = Objects.requireNonNull(auditDao, "auditDao");
    this.auditEventMapper = Objects.requireNonNull(auditEventMapper, "auditEventMapper");
  }

  /**
   * Persists a timestamped audit event in the caller's transaction after cleaning and bounding its
   * details.
   *
   * @param session the Hibernate session used for persistence operations
   * @param type the audit entity type
   * @param id the persistent record identifier
   * @param action the audit action being recorded
   * @param actor the authenticated actor's username
   * @param detail the details to include in the audit event
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws NullPointerException if {@code session} or {@code type} or {@code action} is {@code
   *     null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void recordAuditEvent(
      Session session, AuditEntity type, Long id, AuditAction action, String actor, String detail) {
    AuditEventDto data =
        AuditEventDto.builder()
            .entityType(type.name())
            .entityId(id)
            .actionName(action.name())
            .actor(actor)
            .details(detail)
            .createdAt(new Date())
            .build();
    auditDao.save(session, auditEventMapper.mapToEntity(auditEventValidator.validate(data)));
  }

  /**
   * Requires an active reporting user and returns up to 50 audit events for a zero-based page,
   * newest first.
   *
   * @param actor the authenticated actor's username
   * @param request the requested page index
   * @return up to 50 audit event snapshots for the requested page, newest first
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<AuditEventDto> listRecentAuditEvents(
      final String actor, final PageRequestDto request) {
    final PageRequestDto validated = pageRequestValidator.validate(request);
    return transactionTemplate.execute(session -> {
      accountAccessService.requireReportingPermission(
          accountAccessService.requireActiveAccount(session, actor));
      return auditEventMapper.mapToDtos(
          auditDao.findRecentAuditEvents(session, validated.getPage()));
    });
  }
}
