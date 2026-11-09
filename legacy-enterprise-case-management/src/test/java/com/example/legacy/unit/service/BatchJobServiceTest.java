package com.example.legacy.unit.service;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.CaseDao;
import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.Account;
import com.example.legacy.model.CaseNotice;
import com.example.legacy.model.ServiceCase;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.BatchJobService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.unit.support.ServiceTestSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Behavioral contracts for BatchJobService.
 *
 * @author Chuck Emmons
 */
public class BatchJobServiceTest extends ServiceTestSupport {

  private CaseDao cases;
  private NoticeDao notices;
  private NotificationService notifications;
  private AccountAccessService access;
  private BatchJobService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    cases = mock(CaseDao.class);
    notices = mock(NoticeDao.class);
    notifications = mock(NotificationService.class);
    access = mock(AccountAccessService.class);
    service = new BatchJobService(template, cases, notices, notifications, access);
  }

  /**
   * Notifies the assignee or creator and marks cases in the same transaction.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void notifiesTheAssigneeOrCreatorAndMarksCasesInTheSameTransaction() {
    ServiceCase assigned = new ServiceCase();
    assigned.setId(1L);
    assigned.setAssignedTo("worker");
    assigned.setCaseNumber("CASE-1");
    assigned.setTitle("Assigned");
    ServiceCase unassigned = new ServiceCase();
    unassigned.setId(2L);
    unassigned.setCreatedBy("creator");
    unassigned.setCaseNumber("CASE-2");
    unassigned.setTitle("Unassigned");
    when(cases.findOverdueCasesAwaitingNotification(session))
        .thenReturn(Arrays.asList(assigned, unassigned));
    service.createOverdueNotifications();
    verify(notifications)
        .createNotification(eq(session), eq("worker"), eq(1L), anyString(), eq("Assigned"));
    verify(notifications)
        .createNotification(eq(session), eq("creator"), eq(2L), anyString(), eq("Unassigned"));
    assertTrue(assigned.isOverdueNoticeSent());
    assertTrue(unassigned.isOverdueNoticeSent());
    verify(transaction).commit();
  }

  /**
   * Leaves cases unmarked when notification creation fails.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void leavesCasesUnmarkedWhenNotificationCreationFails() {
    ServiceCase item = new ServiceCase();
    item.setId(7L);
    item.setCreatedBy("creator");
    item.setCaseNumber("CASE-7");
    when(cases.findOverdueCasesAwaitingNotification(session))
        .thenReturn(Collections.singletonList(item));
    doThrow(new IllegalStateException("notification failed"))
        .when(notifications)
        .createNotification(
            eq(session), anyString(), anyLong(), anyString(), nullable(String.class));
    assertThrows(IllegalStateException.class, service::createOverdueNotifications);
    assertFalse(item.isOverdueNoticeSent());
    verify(transaction).rollback();
  }

  /**
   * Skips email when required smtp settings are absent.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void skipsEmailWhenRequiredSmtpSettingsAreAbsent() throws Exception {
    for (String[] setting :
        new String[][] {
          {null, "sender@example.test"},
          {"", "sender@example.test"},
          {"localhost", null},
          {"localhost", ""}
        }) {
      withEnvironmentVariable("SMTP_HOST", setting[0])
          .and("SMTP_FROM", setting[1])
          .execute(service::sendPendingEmail);
    }
    verifyNoInteractions(factory, notices);
  }

  /**
   * Skips missing and already sent notices.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void skipsMissingAndAlreadySentNotices() throws Exception {
    CaseNotice sent = new CaseNotice();
    Date timestamp = new Date(1234);
    sent.setSentAt(timestamp);
    when(notices.findPendingEmailNotificationIds(session)).thenReturn(Arrays.asList(1L, 2L));
    when(notices.findNotificationById(session, 2L)).thenReturn(sent);
    withEnvironmentVariable("SMTP_HOST", "localhost")
        .and("SMTP_FROM", "sender@example.test")
        .execute(service::sendPendingEmail);
    assertSame(timestamp, sent.getSentAt());
    verifyNoInteractions(access);
  }

  /**
   * Marks notices processed when recipients have no email address.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void marksNoticesProcessedWhenRecipientsHaveNoEmailAddress() throws Exception {
    for (String email : new String[] {null, "", "  "}) {
      CaseNotice notice = new CaseNotice();
      notice.setRecipient("worker");
      Account recipient = account("worker", "WORKER");
      recipient.setEmail(email);
      when(notices.findPendingEmailNotificationIds(session))
          .thenReturn(Collections.singletonList(7L));
      when(notices.findNotificationById(session, 7L)).thenReturn(notice);
      when(access.findAccountByUsername(session, "worker")).thenReturn(recipient);
      withEnvironmentVariable("SMTP_HOST", "localhost")
          .and("SMTP_FROM", "sender@example.test")
          .execute(service::sendPendingEmail);
      assertNotNull(notice.getSentAt());
    }
    CaseNotice missingRecipient = new CaseNotice();
    missingRecipient.setRecipient("missing");
    when(notices.findNotificationById(session, 7L)).thenReturn(missingRecipient);
    withEnvironmentVariable("SMTP_HOST", "localhost")
        .and("SMTP_FROM", "sender@example.test")
        .execute(service::sendPendingEmail);
    assertNotNull(missingRecipient.getSentAt());
  }

  /**
   * Configures authenticated smtp and sends utf8 message.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void configuresAuthenticatedSmtpAndSendsUtf8Message() throws Exception {
    CaseNotice notice = new CaseNotice();
    notice.setRecipient("worker");
    notice.setSubject("Subject");
    notice.setMessage("Body");
    Account recipient = account("worker", "WORKER");
    recipient.setEmail("worker@example.test");
    when(notices.findPendingEmailNotificationIds(session))
        .thenReturn(Collections.singletonList(7L));
    when(notices.findNotificationById(session, 7L)).thenReturn(notice);
    when(access.findAccountByUsername(session, "worker")).thenReturn(recipient);
    try (MockedStatic<Transport> transport = mockStatic(Transport.class); ) {
      withEnvironmentVariable("SMTP_HOST", "smtp.example.test")
          .and("SMTP_FROM", "sender@example.test")
          .and("SMTP_PORT", "2525")
          .and("SMTP_USER", "smtp-user")
          .and("SMTP_PASSWORD", "test-password")
          .execute(service::sendPendingEmail);
      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      transport.verify(() -> Transport.send(sent.capture()));
      assertEquals("Subject", sent.getValue().getSubject());
      assertEquals("worker@example.test", sent.getValue().getAllRecipients()[0].toString());
      javax.mail.Session mail = sent.getValue().getSession();
      assertEquals("2525", mail.getProperty("mail.smtp.port"));
      assertEquals("true", mail.getProperty("mail.smtp.starttls.enable"));
      assertEquals("true", mail.getProperty("mail.smtp.auth"));
      javax.mail.PasswordAuthentication credentials =
          mail.requestPasswordAuthentication(null, 2525, "smtp", null, null);
      assertEquals("smtp-user", credentials.getUserName());
      assertEquals("test-password", credentials.getPassword());
      assertNotNull(notice.getSentAt());
    }
  }

  /**
   * Retains failed email for retry and continues with the next notice.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void retainsFailedEmailForRetryAndContinuesWithTheNextNotice() throws Exception {
    CaseNotice failed = new CaseNotice();
    failed.setRecipient("worker");
    failed.setSubject("Fail");
    failed.setMessage("Body");
    CaseNotice success = new CaseNotice();
    success.setRecipient("worker");
    success.setSubject("Success");
    success.setMessage("Body");
    Account recipient = account("worker", "WORKER");
    recipient.setEmail("worker@example.test");
    when(notices.findPendingEmailNotificationIds(session)).thenReturn(Arrays.asList(1L, 2L));
    when(notices.findNotificationById(session, 1L)).thenReturn(failed);
    when(notices.findNotificationById(session, 2L)).thenReturn(success);
    when(access.findAccountByUsername(session, "worker")).thenReturn(recipient);
    try (MockedStatic<Transport> transport = mockStatic(Transport.class); ) {
      transport
          .when(() -> Transport.send(any(Message.class)))
          .thenAnswer(call -> {
            Message message = call.getArgument(0);
            if ("Fail".equals(message.getSubject())) {
              throw new MessagingException("SMTP unavailable");
            }
            return null;
          });
      withEnvironmentVariable("SMTP_HOST", "localhost")
          .and("SMTP_FROM", "sender@example.test")
          .and("SMTP_PORT", null)
          .and("SMTP_USER", null)
          .and("SMTP_PASSWORD", null)
          .execute(service::sendPendingEmail);
      assertNull(failed.getSentAt());
      assertNotNull(success.getSentAt());
      transport.verify(() -> Transport.send(any(Message.class)), times(2));
      verify(transaction).rollback();
    }
  }
}
