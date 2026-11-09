package com.example.legacy.service;

import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.CaseNotice;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.mapper.NoticeMapper;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.service.validation.NoticeDtoValidator;
import com.example.legacy.service.validation.RecordReferenceDtoValidator;
import com.example.legacy.util.MessageKeys;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Creates durable notices within caller-owned transactions and exposes recipient-scoped queries and
 * commands.
 *
 * @author Chuck Emmons
 */
public class NotificationService {
  private final NoticeDtoValidator noticeValidator = new NoticeDtoValidator();
  private final RecordReferenceDtoValidator recordReferenceValidator =
      new RecordReferenceDtoValidator();

  private final HibernateTransactionTemplate transactionTemplate;
  private final AccountAccessService accountAccessService;
  private final NoticeDao noticeDao;
  private final NoticeMapper noticeMapper;

  /** Creates lazy production collaborators without opening a database connection. */
  public NotificationService() {
    this(null);
  }

  /**
   * Wires collaborators to the supplied factory, resolving production configuration lazily when
   * {@code null}.
   *
   * @param sessionFactory the session factory to use, or {@code null} to resolve the shared
   *     application factory lazily
   */
  public NotificationService(SessionFactory sessionFactory) {
    this(
        new HibernateTransactionTemplate(sessionFactory),
        new AccountAccessService(sessionFactory),
        new NoticeDao(),
        new NoticeMapper());
  }

  /**
   * Accepts request-independent collaborators explicitly for testing or alternative wiring.
   *
   * @param transactionTemplate the transaction template that manages session and transaction
   *     lifecycles
   * @param accountAccessService the account access service collaborator
   * @param noticeDao the notice DAO collaborator
   * @param noticeMapper the notice mapper collaborator
   * @throws NullPointerException if {@code transactionTemplate} or {@code accountAccessService} or
   *     {@code noticeDao} or {@code noticeMapper} is {@code null}
   */
  public NotificationService(
      HibernateTransactionTemplate transactionTemplate,
      AccountAccessService accountAccessService,
      NoticeDao noticeDao,
      NoticeMapper noticeMapper) {
    this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    this.accountAccessService =
        Objects.requireNonNull(accountAccessService, "accountAccessService");
    this.noticeDao = Objects.requireNonNull(noticeDao, "noticeDao");
    this.noticeMapper = Objects.requireNonNull(noticeMapper, "noticeMapper");
  }

  /**
   * Persists an unread notification in the caller's transaction, cleaning subject and body; missing
   * or empty recipients are ignored.
   *
   * @param session the Hibernate session used for persistence operations
   * @param username the username identifying the account
   * @param caseId the case identifier
   * @param subject the email subject
   * @param body the message body
   * @throws com.example.legacy.service.exception.BusinessValidationException if a required field is
   *     missing, malformed, or outside its supported range
   * @throws NullPointerException if {@code session} is {@code null}
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  public void createNotification(
      Session session, String username, Long caseId, String subject, String body) {
    if (username == null || username.isEmpty()) {
      return;
    }

    NoticeDto data =
        NoticeDto.builder()
            .recipient(username)
            .caseId(caseId)
            .subject(subject)
            .message(body)
            .createdAt(new Date())
            .build();

    CaseNotice notice = noticeMapper.mapToEntity(noticeValidator.validate(data));
    notice.setRead(false);

    noticeDao.save(session, notice);
  }

  /**
   * Requires an active recipient and returns their latest 100 notifications, including read
   * notifications.
   *
   * @param user the user's username
   * @return the matching notice DTO values
   * @throws AccessDeniedException if the account is unavailable or the actor lacks permission for
   *     this operation
   * @throws com.example.legacy.service.exception.ConcurrentUpdateException if submitted data is
   *     stale or Hibernate detects an optimistic-lock conflict
   * @throws RuntimeException if transaction callback execution, transaction completion, or session
   *     cleanup fails
   * @throws org.hibernate.HibernateException if a database query, persistence operation, or
   *     Hibernate session or transaction operation fails
   */
  @SuppressWarnings("unchecked")
  public List<NoticeDto> listNotifications(final String user) {
    return transactionTemplate.execute(session -> {
      accountAccessService.requireActiveAccount(session, user);
      return noticeMapper.mapToDtos(noticeDao.findNotificationsByUsername(session, user));
    });
  }

  /**
   * Marks a notification read only after validating the active user and matching the notification
   * recipient.
   *
   * @param user the user's username
   * @param request the record identifier
   * @throws AccessDeniedException if the account is unavailable or the notification is missing or
   *     belongs to another recipient
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
  public void markNotificationRead(final String user, final RecordReferenceDto request) {
    final RecordReferenceDto validated = recordReferenceValidator.validate(request);

    transactionTemplate.executeWithoutResult(session -> {
      accountAccessService.requireActiveAccount(session, user);

      CaseNotice notice = noticeDao.findNotificationById(session, validated.getId());

      if (notice == null || !notice.getRecipient().equals(user)) {
        throw new AccessDeniedException(MessageKeys.BUSINESS_NOTIFICATION_NOT_ACCESSIBLE);
      }

      NoticeDto data = noticeMapper.mapToDto(notice);
      data = NoticeDto.builder(data).read(true).build();

      noticeMapper.updateRead(data, notice);
    });
  }
}
