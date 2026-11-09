package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.service.NotificationService;
import com.example.legacy.service.dto.NoticeDto;
import com.example.legacy.service.dto.RecordReferenceDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.NotificationAction;
import com.example.legacy.web.NotificationForm;
import com.example.legacy.web.PageView;
import java.util.Collections;
import org.junit.Test;

/**
 * Behavioral contracts for NotificationAction.
 *
 * @author Chuck Emmons
 */
public class NotificationActionTest extends WebTestSupport {

  /**
   * Lists notifications and marks the submitted record read.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void listsNotificationsAndMarksTheSubmittedRecordRead() {
    NotificationService service = mock(NotificationService.class);
    NotificationForm form = new NotificationForm();
    form.setId("7");
    when(service.listNotifications("admin"))
        .thenReturn(Collections.singletonList(NoticeDto.builder().subject("Subject").build()));
    NotificationAction action = new NotificationAction(service);
    assertEquals(
        PageView.NOTIFICATIONS.getDefinition(),
        action.execute(mapping("list"), form, request, response).getPath());
    assertEquals("Subject", form.getNotices().get(0).getSubject());
    assertTrue(action.execute(mapping("read"), form, request, response).getRedirect());
    verify(service).markNotificationRead(eq("admin"), any(RecordReferenceDto.class));
  }

  /**
   * Presents notification read failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void presentsNotificationReadFailures() {
    NotificationService service = mock(NotificationService.class);
    NotificationForm form = new NotificationForm();
    form.setId("7");
    doThrow(new AccessDeniedException("denied"))
        .when(service)
        .markNotificationRead(eq("admin"), any(RecordReferenceDto.class));
    assertEquals(
        PageView.ERROR.getDefinition(),
        new NotificationAction(service)
            .execute(mapping("read"), form, request, response)
            .getPath());
  }
}
