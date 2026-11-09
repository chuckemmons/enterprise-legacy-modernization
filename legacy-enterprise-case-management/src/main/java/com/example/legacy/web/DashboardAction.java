package com.example.legacy.web;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.ReportPageDto;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Loads dashboard transport data into a request-scoped report form.
 *
 * @author Chuck Emmons
 */
public final class DashboardAction extends Action {
  private final BackOfficeService backOfficeService;

  /** Creates this action and its reusable service collaborators. */
  public DashboardAction() {
    this(new BackOfficeService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param backOfficeService the back office service collaborator
   * @throws NullPointerException if {@code backOfficeService} is {@code null}
   */
  public DashboardAction(BackOfficeService backOfficeService) {
    this.backOfficeService =
        java.util.Objects.requireNonNull(backOfficeService, "backOfficeService");
  }

  /**
   * Converts form input to a DTO and populates dashboard totals from the service response.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect
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
      ReportForm dashboardForm = (ReportForm) form;
      dashboardForm.setData(
          ReportPageDto.builder(dashboardForm.getData())
              .summary(
                  backOfficeService.getDashboardSummary(WebUtil.getAuthenticatedUsername(request)))
              .build());
      return WebUtil.createViewForward(PageView.DASHBOARD);
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    }
  }
}
