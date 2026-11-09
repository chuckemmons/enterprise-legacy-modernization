package com.example.legacy.service;

import com.example.legacy.dao.AccountDao;
import com.example.legacy.domain.Role;
import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.WorkflowUtil;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Loads current accounts and enforces reusable account-level authorization in caller-owned
 * transactions.
 *
 * @author Chuck Emmons
 */
public class AccountAccessService {
  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountDao accountDao;
  private final AccountMapper accountMapper;
  private final CasePolicyService casePolicyService;

  /** Creates lazy production collaborators without opening a database connection. */
  public AccountAccessService() {
    this(null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * null.
   *
   * @param sessionFactory the Hibernate session factory to use
   */
  public AccountAccessService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountDao(),
        new AccountMapper(),
        new CasePolicyService());
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountDao the account DAO collaborator
   * @param accountMapper the account mapper collaborator
   * @param casePolicyService the case policy service collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountDao} or {@code
   *     accountMapper} or {@code casePolicyService} is {@code null}
   */
  public AccountAccessService(
      HibernateTransactionTemplate transactionTemplate,
      AccountDao accountDao,
      AccountMapper accountMapper,
      CasePolicyService casePolicyService) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountDao = Objects.requireNonNull(accountDao, "accountDao");
    this.accountMapper = Objects.requireNonNull(accountMapper, "accountMapper");
    this.casePolicyService = Objects.requireNonNull(casePolicyService, "casePolicyService");
  }

  /**
   * Reloads an enabled account in a service-owned transaction for session revalidation.
   *
   * @param username the username identifying the account
   * @return the account DTO
   * @throws AccessDeniedException if the account does not exist or is disabled
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public AccountDto loadActiveAccount(final String username) {
    return transactionTemplate.execute(
        session -> accountMapper.mapToDto(requireActiveAccount(session, username)));
  }

  /**
   * Looks up an account by username in the supplied session, returning null when absent.
   *
   * @param session the Hibernate session used for persistence operations
   * @param username the username identifying the account
   * @return the account
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public Account findAccountByUsername(Session session, String username) {
    return accountDao.findAccountByUsername(session, username);
  }

  /**
   * Returns an existing enabled account.
   *
   * @param session the Hibernate session used for persistence operations
   * @param username the username identifying the account
   * @return the existing enabled account
   * @throws AccessDeniedException if the account does not exist or is disabled
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public Account requireActiveAccount(Session session, String username) {
    Account user = findAccountByUsername(session, username);
    if (user == null || !user.isEnabled()) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_ACCOUNT_UNAVAILABLE);
    }
    return user;
  }

  /**
   * Requires administrator, supervisor, or auditor access to reporting operations.
   *
   * @param account the account to persist or inspect
   * @throws AccessDeniedException if the account has none of the administrator, supervisor, or
   *     auditor roles
   * @throws NullPointerException if {@code account} is {@code null}
   */
  public void requireReportingPermission(Account account) {
    if (!WorkflowUtil.isSupervisor(account.getRole())
        && !Role.AUDITOR.name().equals(account.getRole())) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_REPORTING_PERMISSION_REQUIRED);
    }
  }

  /**
   * Requires administrator or supervisor privileges, throwing AccessDeniedException otherwise.
   *
   * @param user the user account
   * @throws AccessDeniedException if the account is neither an administrator nor a supervisor
   * @throws NullPointerException if {@code user} is {@code null}
   */
  public void requireSupervisorPermission(Account user) {
    if (!WorkflowUtil.isSupervisor(user.getRole())) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_SUPERVISOR_PERMISSION_REQUIRED);
    }
  }

  /**
   * Requires a role permitted to work on cases, throwing AccessDeniedException for read-only roles.
   *
   * @param user the user account
   * @throws AccessDeniedException if the account has a read-only role
   * @throws NullPointerException if {@code user} is {@code null}
   */
  public void requireCaseWorkPermission(Account user) {
    casePolicyService.requireCaseWorkPermission(user);
  }

  /**
   * Requires the ADMIN role, throwing AccessDeniedException otherwise.
   *
   * @param user the user account
   * @throws AccessDeniedException if the account does not have the administrator role
   * @throws NullPointerException if {@code user} is {@code null}
   */
  public void requireAdministratorPermission(Account user) {
    if (!Role.ADMIN.name().equals(user.getRole())) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_ADMINISTRATOR_PERMISSION_REQUIRED);
    }
  }

  /**
   * Lists accounts ordered by username using the supplied session, including disabled accounts.
   *
   * @param session the Hibernate session used for persistence operations
   * @return the matching account values
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<Account> listAllAccounts(Session session) {
    return accountDao.findAllAccounts(session);
  }
}
