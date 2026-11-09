package com.example.legacy.service;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.domain.Priority;
import com.example.legacy.domain.Role;
import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AssignCaseDto;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CreateCaseDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.CaseDefaults;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import com.example.legacy.util.PasswordsUtil;
import com.example.legacy.util.ValidationLimits;
import java.util.Date;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Provisions explicit CLI seed data through the same case application contracts as web callers.
 *
 * @author Chuck Emmons
 */
public class BootstrapService {
  private static final String DEMO_SUPERVISOR = "supervisor";
  private static final String DEMO_WORKER = "worker";
  private static final String DEMO_AUDITOR = "auditor";
  private static final String DEMO_ACCESS_CATEGORY = "ACCESS";

  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountDao accountDao;
  private final AccountMapper accountMapper;
  private final AccountAccessService accountAccessService;
  private final CaseService caseService;

  /** Creates lazy production collaborators without opening a database connection. */
  public BootstrapService() {
    this((SessionFactory) null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public BootstrapService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountDao(),
        new AccountMapper(),
        new AccountAccessService(sessionFactory),
        new DefaultCaseService(sessionFactory));
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountDao the account DAO collaborator
   * @param accountMapper the account mapper collaborator
   * @param accountAccessService the account access service collaborator
   * @param caseService the case service collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountDao} or {@code
   *     accountMapper} or {@code accountAccessService} or {@code caseService} is {@code null}
   */
  public BootstrapService(
      HibernateTransactionTemplate transactionTemplate,
      AccountDao accountDao,
      AccountMapper accountMapper,
      AccountAccessService accountAccessService,
      CaseService caseService) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountDao = Objects.requireNonNull(accountDao, "accountDao");
    this.accountMapper = Objects.requireNonNull(accountMapper, "accountMapper");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.caseService = Objects.requireNonNull(caseService, "caseService");
  }

  /**
   * Validates bootstrap credentials and creates an administrator only when the username is absent.
   *
   * @param username the username identifying the account
   * @param password the plaintext password
   * @throws BusinessValidationException if the administrator environment variables are missing or
   *     the account fields are invalid
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if the administrator account already exists or password key
   *     derivation fails
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void seedAdmin(final String username, final String password) {
    if (username == null
        || !username.matches(ValidationLimits.USERNAME_PATTERN)
        || password == null
        || password.length() < ValidationLimits.PASSWORD_MIN) {
      throw new BusinessValidationException(
          MessageKeys
              .BUSINESS_SET_CASE_ADMIN_USER_AND_CASE_ADMIN_PASSWORD_12_CHARS_ENVIRONMENT_VARIABLES);
    }

    transactionTemplate.executeWithoutResult(session -> {
      if (accountAccessService.findAccountByUsername(session, username) != null) {
        throw new IllegalStateException(
            MessagesUtil.formatMessage(
                MessageKeys.BUSINESS_ACCOUNT_ALREADY_EXISTS_NO_CHANGES_MADE));
      }

      saveAccount(
          session,
          username,
          password,
          Role.ADMIN.name(),
          MessagesUtil.formatMessage(MessageKeys.SEED_ADMIN_DISPLAY_NAME));
    });
  }

  /**
   * Creates missing demo accounts and eight assigned sample cases; existing account credentials are
   * preserved.
   *
   * @param password the plaintext password
   * @throws com.example.legacy.service.exception.AccessDeniedException if the account is
   *     unavailable or the actor lacks permission for this operation
   * @throws BusinessValidationException if the demo password is missing or seeded account or case
   *     data fails validation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws com.example.legacy.service.exception.RecordNotFoundException if the requested case does
   *     not exist or is archived
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if PBKDF2 password key derivation is unavailable or fails
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void seedDemo(final String password) {
    if (password == null || password.length() < ValidationLimits.PASSWORD_MIN) {
      throw new BusinessValidationException(
          MessageKeys.BUSINESS_SET_CASE_DEMO_PASSWORD_TO_A_NONTRIVIAL_PASSWORD_12_CHARS);
    }

    transactionTemplate.executeWithoutResult(session -> {
      for (String[] row :
          new String[][] {
            {DEMO_SUPERVISOR, Role.SUPERVISOR.name()},
            {DEMO_WORKER, Role.WORKER.name()},
            {DEMO_AUDITOR, Role.AUDITOR.name()}
          }) {
        if (accountAccessService.findAccountByUsername(session, row[0]) == null) {
          saveAccount(
              session,
              row[0],
              password,
              row[1],
              MessagesUtil.formatMessage(MessageKeys.SEED_USER_DISPLAY_NAME, row[1]));
        }
      }
    });

    for (int i = 0; i < 8; i++) {
      CaseDto item =
          caseService.createCase(
              DEMO_WORKER,
              CreateCaseDto.builder()
                  .title(MessagesUtil.formatMessage(MessageKeys.SEED_CASE_TITLE, i + 1))
                  .description(MessagesUtil.formatMessage(MessageKeys.SEED_CASE_DESCRIPTION))
                  .category(i % 2 == 0 ? DEMO_ACCESS_CATEGORY : CaseDefaults.CATEGORY)
                  .priority(i % 3 == 0 ? Priority.HIGH.name() : Priority.MEDIUM.name())
                  .department(CaseDefaults.DEPARTMENT)
                  .dueAt(new Date(System.currentTimeMillis() + 86400000L * (i - 2)))
                  .build());

      caseService.assignCase(
          DEMO_SUPERVISOR, AssignCaseDto.builder().id(item.getId()).assignee(DEMO_WORKER).build());
    }
  }

  /**
   * Persists a validated seed account with its hashed password and standard default metadata.
   *
   * @param session the Hibernate session used for persistence operations
   * @param username the username identifying the account
   * @param password the plaintext password
   * @param role the account role code
   * @param displayName the account's display name
   * @throws BusinessValidationException if a required field is missing, malformed, or outside its
   *     supported range
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if PBKDF2 password key derivation is unavailable or fails
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void saveAccount(
      Session session, String username, String password, String role, String displayName) {
    AccountDto input =
        AccountDto.builder()
            .username(username)
            .role(role)
            .displayName(displayName)
            .email("")
            .department(CaseDefaults.DEPARTMENT)
            .build();

    Account entity = accountMapper.mapToEntity(input);
    entity.setPasswordHash(PasswordsUtil.hashPassword(password));
    entity.setEnabled(true);
    entity.setCreatedAt(new Date());

    accountDao.save(session, entity);
  }
}
