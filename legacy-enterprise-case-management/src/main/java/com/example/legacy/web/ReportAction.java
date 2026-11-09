package com.example.legacy.web;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.ReportPageDto;
import com.example.legacy.util.ContentTypes;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import com.example.legacy.util.StringsUtil;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Presents operational reports and serializes entity-free case DTOs as CSV.
 *
 * @author Chuck Emmons
 */
public final class ReportAction extends Action {
  private final BackOfficeService backOfficeService;

  /** Creates this action and its reusable service collaborators. */
  public ReportAction() {
    this(new BackOfficeService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param backOfficeService the back office service collaborator
   * @throws NullPointerException if {@code backOfficeService} is {@code null}
   */
  public ReportAction(BackOfficeService backOfficeService) {
    this.backOfficeService =
        java.util.Objects.requireNonNull(backOfficeService, "backOfficeService");
  }

  /**
   * Maps the report form to service input, populates it from DTO results, and renders or exports
   * its data.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect, or {@code null} when the response is written
   *     directly
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Override
  public ActionForward execute(
      ActionMapping mapping,
      ActionForm form,
      HttpServletRequest request,
      HttpServletResponse response) {
    try {
      ReportForm reportForm = (ReportForm) form;
      ReportPageDto data = reportForm.getData();
      String actor = WebUtil.getAuthenticatedUsername(request);
      if (ActionOperation.PAGE.matchesParameter(mapping.getParameter())) {
        reportForm.setData(
            ReportPageDto.builder(data).summary(backOfficeService.getReportSummary(actor)).build());
        return WebUtil.createViewForward(PageView.REPORTS);
      }
      reportForm.setData(
          ReportPageDto.builder(data).cases(backOfficeService.exportCases(actor)).build());
      response.setContentType(ContentTypes.CSV_UTF8);
      response.setHeader(HttpConstants.CONTENT_DISPOSITION, HttpConstants.CASE_EXPORT_DISPOSITION);
      PrintWriter writer = response.getWriter();
      writer.println(
          MessagesUtil.formatMessage(
              WebUtil.resolveLocale(request), MessageKeys.REPORT_CSV_HEADER));
      for (CaseDto item : reportForm.getCases()) {
        writer.println(
            StringsUtil.escapeCsvCell(item.getCaseNumber())
                + ","
                + StringsUtil.escapeCsvCell(item.getTitle())
                + ","
                + StringsUtil.escapeCsvCell(item.getCategory())
                + ","
                + StringsUtil.escapeCsvCell(item.getPriority())
                + ","
                + StringsUtil.escapeCsvCell(item.getStatus())
                + ","
                + StringsUtil.escapeCsvCell(item.getAssignedTo())
                + ","
                + StringsUtil.escapeCsvCell(item.getDepartment()));
      }
      return null;
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    }
  }
}
