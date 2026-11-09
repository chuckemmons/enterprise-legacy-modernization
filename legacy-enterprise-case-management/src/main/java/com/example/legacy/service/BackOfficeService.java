package com.example.legacy.service;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.dao.CaseDao;
import com.example.legacy.domain.AuditAction;
import com.example.legacy.domain.AuditEntity;
import com.example.legacy.domain.ReportGrouping;
import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.dto.ChangeAccountRoleDto;
import com.example.legacy.service.dto.CountDto;
import com.example.legacy.service.dto.CreateAccountDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.RecordNotFoundException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.mapper.CaseMapper;
import com.example.legacy.service.mapper.SummaryMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.ChangeAccountRoleDtoValidator;
import com.example.legacy.service.validation.CreateAccountDtoValidator;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import com.example.legacy.util.PasswordsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.hibernate.SessionFactory;

/**
 * Applies administrator account commands and authorized dashboard/report queries.
 *
 * @author Chuck Emmons
 */
public class BackOfficeService {
  private final CreateAccountDtoValidator createAccountValidator = new CreateAccountDtoValidator();
  private final ChangeAccountRoleDtoValidator changeAccountRoleValidator =
      new ChangeAccountRoleDtoValidator();
  private final RecordReferenceDtoValidator recordReferenceValidator =
      new RecordReferenceDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountDao accountDao;
  private final CaseDao caseDao;
  private final AccountAccessService accountAccessService;
  private final AuditService auditService;
  private final AccountMapper accountMapper;
  private final CaseMapper caseMapper;
  private final SummaryMapper summaryMapper;

  /** Creates lazy production collaborators without opening a database connection. */
  public BackOfficeService() {
    this((SessionFactory) null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public BackOfficeService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountDao(),
        new CaseDao(),
        new AccountAccessService(sessionFactory),
        new AuditService(sessionFactory),
        new AccountMapper(),
        new CaseMapper(),
        new SummaryMapper());
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountDao the account DAO collaborator
   * @param caseDao the case DAO collaborator
   * @param accountAccessService the account access service collaborator
   * @param auditService the audit service collaborator
   * @param accountMapper the account mapper collaborator
   * @param caseMapper the case mapper collaborator
   * @param summaryMapper the summary mapper collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountDao} or {@code
   *     caseDao} or {@code accountAccessService} or {@code auditService} or {@code accountMapper}
   *     or {@code caseMapper} or {@code summaryMapper} is {@code null}
   */
  public BackOfficeService(
      HibernateTransactionTemplate transactionTemplate,
      AccountDao accountDao,
      CaseDao caseDao,
      AccountAccessService accountAccessService,
      AuditService auditService,
      AccountMapper accountMapper,
      CaseMapper caseMapper,
      SummaryMapper summaryMapper) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountDao = Objects.requireNonNull(accountDao, "accountDao");
    this.caseDao = Objects.requireNonNull(caseDao, "caseDao");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.auditService = Objects.requireNonNull(auditService, "auditService");
    this.accountMapper = Objects.requireNonNull(accountMapper, "accountMapper");
    this.caseMapper = Objects.requireNonNull(caseMapper, "caseMapper");
    this.summaryMapper = Objects.requireNonNull(summaryMapper, "summaryMapper");
  }

  /**
   * Requires an active administrator, validates a unique normalized username and account fields,
   * hashes the initial password, and saves the account with an audit event.
   *
   * @param actor the authenticated actor's username
   * @param request the initial account details and password
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if account fields are invalid or the username is already
   *     taken
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if PBKDF2 password key derivation is unavailable or fails
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void createUser(final String actor, final CreateAccountDto request) {
    final CreateAccountDto validated = createAccountValidator.validate(request);
    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireAdministratorPermission(
          accountAccessService.requireActiveAccount(session, actor));
      String username = validated.getUsername();
      if (accountAccessService.findAccountByUsername(session, username) != null) {
        throw new BusinessValidationException(MessageKeys.BUSINESS_USERNAME_IS_ALREADY_TAKEN);
      }
      AccountDto input =
          AccountDto.builder()
              .username(username)
              .displayName(validated.getDisplayName())
              .email(validated.getEmail())
              .role(validated.getRole())
              .department(validated.getDepartment())
              .build();

      Account entity = accountMapper.mapToEntity(input);
      entity.setPasswordHash(PasswordsUtil.hashPassword(validated.getPassword()));
      entity.setEnabled(true);
      entity.setCreatedAt(new Date());

      accountDao.save(session, entity);
      accountDao.flush(session);

      auditService.recordAuditEvent(
          session,
          AuditEntity.ACCOUNT,
          entity.getId(),
          AuditAction.CREATE,
          actor,
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_ACCOUNT_CREATED, username, validated.getRole()));
    });
  }

  /**
   * Requires an active administrator to change another account's role to a supported value and
   * records the previous and new roles in the audit log.
   *
   * @param actor the authenticated actor's username
   * @param request the account reference and requested role
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if the requested account or role is invalid, or the actor
   *     attempts to change their own role
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RecordNotFoundException if the requested account does not exist
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void changeUserRole(final String actor, final ChangeAccountRoleDto request) {
    final ChangeAccountRoleDto validated = changeAccountRoleValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireAdministratorPermission(
          accountAccessService.requireActiveAccount(session, actor));

      Account entity = accountDao.findAccountById(session, id);

      if (entity == null) {
        throw new RecordNotFoundException(MessageKeys.BUSINESS_UNKNOWN_USER);
      }
      if (entity.getUsername().equals(actor)) {
        throw new BusinessValidationException(MessageKeys.BUSINESS_CANNOT_CHANGE_YOUR_OWN_ROLE);
      }

      String previousRole = entity.getRole();
      AccountDto data = accountMapper.mapToDto(entity);
      data = AccountDto.builder(data).role(validated.getRole()).build();
      accountMapper.updateRole(data, entity);

      auditService.recordAuditEvent(
          session,
          AuditEntity.ACCOUNT,
          id,
          AuditAction.ROLE_CHANGE,
          actor,
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_ACCOUNT_ROLE,
              entity.getUsername(),
              previousRole,
              validated.getRole()));
    });
  }

  /**
   * Requires an active administrator to toggle another account's enabled flag and audit the change;
   * self-disable is rejected.
   *
   * @param actor the authenticated actor's username
   * @param request the record identifier
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if the account identifier is invalid or the actor attempts
   *     to disable their own account
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RecordNotFoundException if the requested account does not exist
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void toggleUserEnabled(final String actor, final RecordReferenceDto request) {
    final RecordReferenceDto validated = recordReferenceValidator.validate(request);
    final long id = validated.getId();

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireAdministratorPermission(
          accountAccessService.requireActiveAccount(session, actor));

      Account entity = accountDao.findAccountById(session, id);

      if (entity == null) {
        throw new RecordNotFoundException(MessageKeys.BUSINESS_UNKNOWN_USER);
      }
      if (entity.getUsername().equals(actor)) {
        throw new BusinessValidationException(MessageKeys.BUSINESS_CANNOT_DISABLE_YOURSELF);
      }

      AccountDto data = accountMapper.mapToDto(entity);
      data = AccountDto.builder(data).enabled(!data.isEnabled()).build();
      accountMapper.updateEnabled(data, entity);

      auditService.recordAuditEvent(
          session,
          AuditEntity.ACCOUNT,
          id,
          AuditAction.ENABLE,
          actor,
          MessagesUtil.formatMessage(
              MessageKeys.AUDIT_ACCOUNT_ENABLED, entity.getUsername(), entity.isEnabled()));
    });
  }

  /**
   * Returns accounts for an active administrator, ordered by username.
   *
   * @param actor the authenticated actor's username
   * @return accounts for an active administrator, ordered by username
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public List<AccountDto> listAllAccounts(final String actor) {
    return transactionTemplate.execute(session -> {
      accountAccessService.requireAdministratorPermission(
          accountAccessService.requireActiveAccount(session, actor));

      return accountMapper.mapToDtos(accountDao.findAllAccounts(session));
    });
  }

  /**
   * Returns dashboard totals for an active account without requiring reporting privileges.
   *
   * @param actor the authenticated actor's username
   * @return dashboard totals for an active account without requiring reporting privileges
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public CaseSummary getDashboardSummary(final String actor) {
    return buildCaseSummary(actor, false);
  }

  /**
   * Returns status, priority, and overdue totals for an active reporting user.
   *
   * @param actor the authenticated actor's username
   * @return status, priority, and overdue totals for an active reporting user
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public CaseSummary getReportSummary(final String actor) {
    return buildCaseSummary(actor, true);
  }

  /**
   * Returns at most 5,000 active cases for a reporting user; response serialization belongs to the
   * web layer.
   *
   * @param actor the authenticated actor's username
   * @return at most 5,000 active cases for a reporting user; response serialization belongs to the
   *     web layer
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public List<CaseDto> exportCases(final String actor) {
    return transactionTemplate.execute(session -> {
      accountAccessService.requireReportingPermission(
          accountAccessService.requireActiveAccount(session, actor));

      return caseMapper.mapToDtos(
          caseDao.searchCases(session, null, null, null, 0, ValidationLimits.EXPORT_ROWS));
    });
  }

  /**
   * Loads grouped counts in one transaction, applying the permissions required by the use case.
   *
   * @param actor the authenticated actor's username
   * @param reporting whether reporting data should be included
   * @return the case summary
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private CaseSummary buildCaseSummary(final String actor, final boolean reporting) {
    return transactionTemplate.execute(session -> {
      Account account = accountAccessService.requireActiveAccount(session, actor);
      if (reporting) {
        accountAccessService.requireReportingPermission(account);
      }

      CaseDao cases = caseDao;

      return CaseSummary.builder()
          .statuses(
              summaryMapper.mapToDtos(cases.countCasesByGrouping(session, ReportGrouping.STATUS)))
          .priorities(
              reporting
                  ? summaryMapper.mapToDtos(
                      cases.countCasesByGrouping(session, ReportGrouping.PRIORITY))
                  : Collections.<CountDto>emptyList())
          .overdue(cases.countOverdueCases(session))
          .build();
    });
  }
}
