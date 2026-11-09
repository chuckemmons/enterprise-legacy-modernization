package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.service.AuditService;
import com.example.legacy.service.dto.AuditEventDto;
import com.example.legacy.service.dto.PageRequestDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.AuditAction;
import com.example.legacy.web.AuditForm;
import com.example.legacy.web.PageView;
import java.util.Collections;
import org.junit.Test;

/**
 * Behavioral contracts for AuditAction.
 *
 * @author Chuck Emmons
 */
public class AuditActionTest extends WebTestSupport {

  /**
   * Preserves page selection while loading audit events.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void preservesPageSelectionWhileLoadingAuditEvents() {
    AuditService service = mock(AuditService.class);
    when(service.listRecentAuditEvents(eq("admin"), any(PageRequestDto.class)))
        .thenReturn(Collections.singletonList(AuditEventDto.builder().details("Recorded").build()));
    AuditForm form = new AuditForm();
    form.setPageNumber("2");
    assertEquals(
        PageView.AUDIT.getDefinition(),
        new AuditAction(service).execute(mapping("list"), form, request, response).getPath());
    assertEquals("Recorded", form.getEvents().get(0).getDetails());
    org.mockito.ArgumentCaptor<PageRequestDto> page =
        org.mockito.ArgumentCaptor.forClass(PageRequestDto.class);
    verify(service).listRecentAuditEvents(eq("admin"), page.capture());
    assertEquals(2, page.getValue().getPage());
  }

  /**
   * Presents audit load failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void presentsAuditLoadFailures() {
    AuditService service = mock(AuditService.class);
    when(service.listRecentAuditEvents(eq("admin"), any(PageRequestDto.class)))
        .thenThrow(new AccessDeniedException("denied"));
    assertEquals(
        PageView.ERROR.getDefinition(),
        new AuditAction(service)
            .execute(mapping("list"), new AuditForm(), request, response)
            .getPath());
  }
}
