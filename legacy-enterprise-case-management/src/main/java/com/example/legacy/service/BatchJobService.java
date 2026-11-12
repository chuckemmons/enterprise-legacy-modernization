package com.example.legacy.service;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.Account;
import com.example.legacy.model.CaseNotice;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.util.EnvironmentKeys;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Runs explicit overdue and best-effort SMTP jobs with separate bounded transaction units.
 *
 * @author Chuck Emmons
 */
public class BatchJobService {
  private static final String SMTP_HOST_PROPERTY = "mail.smtp.host";
  private static final String SMTP_PORT_PROPERTY = "mail.smtp.port";
  private static final String SMTP_TLS_PROPERTY = "mail.smtp.starttls.enable";
  private static final String SMTP_AUTH_PROPERTY = "mail.smtp.auth";
  private static final String DEFAULT_SMTP_PORT = "25";

  private final HibernateTransactionTemplate transactionTemplate;
  private final CaseDao caseDao;
  private final NoticeDao noticeDao;
  private final NotificationService notificationService;
  private final AccountAccessService accountAccessService;

  /** Creates lazy production collaborators without opening a database connection. */
  public BatchJobService() {
    this((SessionFactory) null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public BatchJobService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new CaseDao(),
        new NoticeDao(),
        new NotificationService(sessionFactory),
        new AccountAccessService(sessionFactory));
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param caseDao the case DAO collaborator
   * @param noticeDao the notice DAO collaborator
   * @param notificationService the notification service collaborator
   * @param accountAccessService the account access service collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code caseDao} or {@code
   *     noticeDao} or {@code notificationService} or {@code accountAccessService} is {@code null}
   */
  public BatchJobService(
      HibernateTransactionTemplate transactionTemplate,
      CaseDao caseDao,
      NoticeDao noticeDao,
      NotificationService notificationService,
      AccountAccessService accountAccessService) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.caseDao = Objects.requireNonNull(caseDao, "caseDao");
    this.noticeDao = Objects.requireNonNull(noticeDao, "noticeDao");
    this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
  }

  /**
   * Processes up to 300 overdue cases not yet flagged, notifying the assignee or creator and
   * marking each case in the same transaction.
   *
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public void createOverdueNotifications() {
    transactionTemplate.executeWithoutResult(
        session -> {
          List<ServiceCase> cases = caseDao.findOverdueCasesAwaitingNotification(session);
          for (ServiceCase serviceCase : cases) {
            String username =
                serviceCase.getAssignedTo() == null
                    ? serviceCase.getCreatedBy()
                    : serviceCase.getAssignedTo();

            notificationService.createNotification(
                session,
                username,
                serviceCase.getId(),
                MessagesUtil.formatMessage(
                    MessageKeys.NOTICE_CASE_OVERDUE, serviceCase.getCaseNumber()),
                serviceCase.getTitle());

            serviceCase.setOverdueNoticeSent(true);
          }
        });
  }

  /**
   * Skips delivery unless SMTP_HOST and SMTP_FROM are set, then attempts up to 50 pending notices.
   * Delivery failures are logged for retry; notices without an email recipient are marked
   * processed.
   *
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public void sendPendingEmail() {
    final String host = System.getenv(EnvironmentKeys.SMTP_HOST);
    final String senderAddress = System.getenv(EnvironmentKeys.SMTP_FROM);

    if (host == null || host.isEmpty() || senderAddress == null || senderAddress.isEmpty()) {
      return;
    }

    List<Long> ids =
        transactionTemplate.execute(session -> noticeDao.findPendingEmailNotificationIds(session));

    for (final Long id : ids) {
      try {
        // Best-effort email. Durable in-app alerts remain if SMTP fails.
        transactionTemplate.executeWithoutResult(
            session -> deliverPendingEmail(session, id, host, senderAddress));
      } catch (RuntimeException failure) {
        com.example.legacy.observability.Telemetry.failure(failure);
      }
    }
  }

  /**
   * Sends or marks one pending notice inside the caller's service-owned transaction.
   *
   * @param session the Hibernate session used for persistence operations
   * @param id the persistent record identifier
   * @param host the SMTP server hostname
   * @param senderAddress the sender's email address
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws IllegalStateException if SMTP message delivery fails
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  private void deliverPendingEmail(Session session, Long id, String host, String senderAddress) {

    CaseNotice notice = noticeDao.findNotificationById(session, id);

    if (notice == null || notice.getSentAt() != null) {
      return;
    }

    Account recipient = accountAccessService.findAccountByUsername(session, notice.getRecipient());

    if (recipient == null
        || recipient.getEmail() == null
        || recipient.getEmail().trim().isEmpty()) {
      notice.setSentAt(new Date());
      return;
    }

    Properties mailProperties = new Properties();
    mailProperties.put(SMTP_HOST_PROPERTY, host);
    mailProperties.put(
        SMTP_PORT_PROPERTY,
        System.getenv(EnvironmentKeys.SMTP_PORT) == null
            ? DEFAULT_SMTP_PORT
            : System.getenv(EnvironmentKeys.SMTP_PORT));
    mailProperties.put(SMTP_TLS_PROPERTY, Boolean.TRUE.toString());

    String user = System.getenv(EnvironmentKeys.SMTP_USER);
    final String password = System.getenv(EnvironmentKeys.SMTP_PASSWORD);

    if (user != null && !user.isEmpty()) {
      mailProperties.put(SMTP_AUTH_PROPERTY, Boolean.TRUE.toString());
    }

    final String smtpUser = user;
    javax.mail.Session mailSession =
        javax.mail.Session.getInstance(
            mailProperties,
            user == null
                ? null
                : new Authenticator() {
                  /**
                   * Supplies the configured SMTP username and password when the mail transport
                   * requests credentials.
                   *
                   * @return the SMTP authentication credentials
                   */
                  @Override
                  protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, password);
                  }
                });

    try {
      MimeMessage message = new MimeMessage(mailSession);
      message.setFrom(new InternetAddress(senderAddress));
      message.setRecipients(
          Message.RecipientType.TO, InternetAddress.parse(recipient.getEmail(), true));
      message.setSubject(notice.getSubject(), java.nio.charset.StandardCharsets.UTF_8.name());
      message.setText(notice.getMessage(), java.nio.charset.StandardCharsets.UTF_8.name());
      Transport.send(message);
    } catch (MessagingException failure) {
      throw new IllegalStateException(
          MessagesUtil.formatMessage(MessageKeys.BUSINESS_SMTP_FAILURE), failure);
    }

    notice.setSentAt(new Date());
  }
}
