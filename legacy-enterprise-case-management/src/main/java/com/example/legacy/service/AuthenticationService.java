package com.example.legacy.service;

import com.example.legacy.model.Account;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.service.mapper.AccountMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.AuthDtoValidator;
import com.example.legacy.util.PasswordsUtil;
import java.util.Objects;
import org.hibernate.SessionFactory;

/**
 * Authenticates credentials and returns credential-free responses without HTTP or page
 * dependencies.
 *
 * @author Chuck Emmons
 */
public class AuthenticationService {
  private final AuthDtoValidator authValidator = new AuthDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountAccessService accountAccessService;
  private final AccountMapper accountMapper;

  /** Creates lazy production collaborators without opening a database connection. */
  public AuthenticationService() {
    this(null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public AuthenticationService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountAccessService(sessionFactory),
        new AccountMapper());
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountAccessService the account access service collaborator
   * @param accountMapper the account mapper collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountAccessService} or
   *     {@code accountMapper} is {@code null}
   */
  public AuthenticationService(
      HibernateTransactionTemplate transactionTemplate,
      AccountAccessService accountAccessService,
      AccountMapper accountMapper) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.accountMapper = Objects.requireNonNull(accountMapper, "accountMapper");
  }

  /**
   * Checks an enabled account and its password in a transaction; returns the detached account on
   * success or {@code null} for invalid credentials.
   *
   * @param username the username identifying the account
   * @param password the plaintext password
   * @return the authenticated account snapshot, or {@code null} for invalid credentials
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private AccountDto authenticate(final String username, final String password) {
    return transactionTemplate.execute(session -> {
      Account account = accountAccessService.findAccountByUsername(session, username);
      AccountDto result = null;
      if (account != null
          && account.isEnabled()
          && PasswordsUtil.matchesPassword(password, account.getPasswordHash())) {
        result = accountMapper.mapToDto(account);
      }
      return result;
    });
  }

  /**
   * Authenticates bound credentials and returns a credential-free account DTO to the HTTP adapter.
   *
   * @param request the submitted username and password
   * @return the credential-free response containing the account, or a {@code null} account for
   *     invalid credentials
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
  public AuthDto authenticate(AuthDto request) {
    AuthDto validated = authValidator.validate(request);
    AccountDto account = authenticate(validated.getUsername(), validated.getPassword());

    return AuthDto.builder().username(request.getUsername()).account(account).build();
  }
}
