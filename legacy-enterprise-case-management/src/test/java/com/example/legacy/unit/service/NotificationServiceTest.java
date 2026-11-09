package com.example.legacy.unit.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.dao.NoticeDao;
import com.example.legacy.model.CaseNotice;
import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.mapper.NoticeMapper;
import com.example.legacy.unit.support.ServiceTestSupport;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Behavioral contracts for NotificationService.
 *
 * @author Chuck Emmons
 */
public class NotificationServiceTest extends ServiceTestSupport {

  private NoticeDao notices;
  private AccountAccessService access;
  private NotificationService service;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createService() {
    notices = mock(NoticeDao.class);
    access = mock(AccountAccessService.class);
    service = new NotificationService(template, access, notices, new NoticeMapper());
  }

  /**
   * Skips missing recipients and persists validated unread notices.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void skipsMissingRecipientsAndPersistsValidatedUnreadNotices() {
    service.createNotification(session, null, 7L, "Subject", "Body");
    service.createNotification(session, "", 7L, "Subject", "Body");
    verifyNoInteractions(notices);
    service.createNotification(session, "worker", 7L, "Subject", "Body");
    ArgumentCaptor<CaseNotice> saved = ArgumentCaptor.forClass(CaseNotice.class);
    verify(notices).save(eq(session), saved.capture());
    assertEquals("worker", saved.getValue().getRecipient());
    assertFalse(saved.getValue().isRead());
    assertNotNull(saved.getValue().getCreatedAt());
  }

  /**
   * Lists detached notifications for the authenticated recipient.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void listsDetachedNotificationsForTheAuthenticatedRecipient() {
    CaseNotice notice = new CaseNotice();
    notice.setRecipient("worker");
    notice.setSubject("Subject");
    when(notices.findNotificationsByUsername(session, "worker"))
        .thenReturn(Collections.singletonList(notice));
    assertEquals("Subject", service.listNotifications("worker").get(0).getSubject());
    verify(access).requireActiveAccount(session, "worker");
    verify(transaction).commit();
  }

  /**
   * Marks only the recipients own notification read.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void marksOnlyTheRecipientsOwnNotificationRead() {
    CaseNotice notice = new CaseNotice();
    notice.setRecipient("worker");
    notice.setSubject("Subject");
    when(notices.findNotificationById(session, 7L)).thenReturn(notice);
    service.markNotificationRead("worker", RecordReferenceDto.builder().id(7).build());
    assertTrue(notice.isRead());
    notice.setRecipient("another");
    notice.setRead(false);
    assertThrows(
        AccessDeniedException.class,
        () -> service.markNotificationRead("worker", RecordReferenceDto.builder().id(7).build()));
    assertFalse(notice.isRead());
    assertThrows(
        AccessDeniedException.class,
        () -> service.markNotificationRead("worker", RecordReferenceDto.builder().id(8).build()));
  }
}
