package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.DashboardAction;
import com.example.legacy.web.PageView;
import com.example.legacy.web.ReportForm;
import org.junit.Test;

/**
 * Behavioral contracts for DashboardAction.
 *
 * @author Chuck Emmons
 */
public class DashboardActionTest extends WebTestSupport {

  /**
   * Loads dashboard summary and presents failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void loadsDashboardSummaryAndPresentsFailures() {
    BackOfficeService service = mock(BackOfficeService.class);
    CaseSummary summary = CaseSummary.builder().overdue(4L).build();
    when(service.getDashboardSummary("admin")).thenReturn(summary);
    ReportForm form = new ReportForm();
    assertEquals(
        PageView.DASHBOARD.getDefinition(),
        new DashboardAction(service).execute(mapping("page"), form, request, response).getPath());
    assertSame(summary, form.getSummary());
    when(service.getDashboardSummary("admin")).thenThrow(new AccessDeniedException("denied"));
    assertEquals(
        PageView.ERROR.getDefinition(),
        new DashboardAction(service).execute(mapping("page"), form, request, response).getPath());
  }
}
