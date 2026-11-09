package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CaseSummary;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.HttpConstants;
import com.example.legacy.web.PageView;
import com.example.legacy.web.ReportAction;
import com.example.legacy.web.ReportForm;
import java.util.Collections;
import org.junit.Test;

/**
 * Behavioral contracts for ReportAction.
 *
 * @author Chuck Emmons
 */
public class ReportActionTest extends WebTestSupport {

  /**
   * Loads report summary for the page operation.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void loadsReportSummaryForThePageOperation() {
    BackOfficeService service = mock(BackOfficeService.class);
    CaseSummary summary = CaseSummary.builder().overdue(3L).build();
    when(service.getReportSummary("admin")).thenReturn(summary);
    ReportForm form = new ReportForm();
    assertEquals(
        PageView.REPORTS.getDefinition(),
        new ReportAction(service).execute(mapping("page"), form, request, response).getPath());
    assertSame(summary, form.getSummary());
  }

  /**
   * Exports quoted csv with formula protection and download headers.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void exportsQuotedCsvWithFormulaProtectionAndDownloadHeaders() throws Exception {
    BackOfficeService service = mock(BackOfficeService.class);
    when(service.exportCases("admin"))
        .thenReturn(
            Collections.singletonList(
                CaseDto.builder()
                    .caseNumber("CASE-7")
                    .title("=formula,quoted")
                    .category("ACCESS")
                    .priority("HIGH")
                    .status("NEW")
                    .department("Operations")
                    .build()));
    assertNull(
        new ReportAction(service).execute(mapping("csv"), new ReportForm(), request, response));
    assertTrue(text.toString().contains("\"'=formula,quoted\""));
    assertTrue(text.toString().contains("CASE-7"));
    verify(response)
        .setHeader(HttpConstants.CONTENT_DISPOSITION, HttpConstants.CASE_EXPORT_DISPOSITION);
  }

  /**
   * Reports authorization failures without writing csv.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reportsAuthorizationFailuresWithoutWritingCsv() {
    BackOfficeService service = mock(BackOfficeService.class);
    when(service.getReportSummary("admin")).thenThrow(new AccessDeniedException("denied"));
    assertEquals(
        PageView.ERROR.getDefinition(),
        new ReportAction(service)
            .execute(mapping("page"), new ReportForm(), request, response)
            .getPath());
    assertEquals("", text.toString());
  }
}
