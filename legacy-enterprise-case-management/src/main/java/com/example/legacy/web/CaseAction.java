package com.example.legacy.web;

import com.example.legacy.service.CaseService;
import com.example.legacy.service.DefaultCaseService;
import com.example.legacy.service.dto.CaseDetails;
import com.example.legacy.service.dto.CaseDto;
import com.example.legacy.service.dto.CasePageDto;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.ConcurrentUpdateException;
import com.example.legacy.util.MessageKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Routes case forms and DTOs between the presentation and service layers.
 *
 * @author Chuck Emmons
 */
public final class CaseAction extends Action {
  private final CaseService caseService;

  /** Creates this action and its reusable service collaborators. */
  public CaseAction() {
    this(new DefaultCaseService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param caseService the case service collaborator
   * @throws NullPointerException if {@code caseService} is {@code null}
   */
  public CaseAction(CaseService caseService) {
    this.caseService = java.util.Objects.requireNonNull(caseService, "caseService");
  }

  /**
   * Maps a bound case form to DTO input and maps service DTO results back to the form.
   *
   * @param mapping the Struts mapping for the current operation
   * @param form the form containing the submitted values
   * @param request the current HTTP request
   * @param response the HTTP response to populate
   * @return the next page forward or redirect
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code mapping} or {@code request} is {@code null}
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
      CaseForm caseForm = (CaseForm) form;
      String actor = WebUtil.getAuthenticatedUsername(request);
      String operation = mapping.getParameter();

      if (ActionOperation.LIST.matchesParameter(operation)) {
        caseForm.setData(
            CasePageDto.builder()
                .query(caseForm.getQ())
                .status(caseForm.getStatus())
                .assignee(caseForm.getAssignee())
                .page(FormDataUtil.parsePageIndex(caseForm.getPageNumber()))
                .cases(caseService.searchCases(actor, caseForm.getSearchData()))
                .build());
        return WebUtil.createViewForward(PageView.CASES);
      }

      if (ActionOperation.NEW.matchesParameter(operation)) {
        return WebUtil.createViewForward(PageView.CASE_NEW);
      }

      if (ActionOperation.VIEW.matchesParameter(operation)) {
        CaseDetails details = caseService.getCaseDetails(actor, caseForm.getRecordData());
        caseForm.setData(
            CasePageDto.builder()
                .id(details.getItem().getId())
                .item(details.getItem())
                .details(details)
                .build());
        return WebUtil.createViewForward(PageView.CASE_VIEW);
      }

      if (ActionOperation.CREATE.matchesParameter(operation)) {
        CaseDto created = caseService.createCase(actor, caseForm.getCreateData());
        caseForm.setData(CasePageDto.builder().id(created.getId()).item(created).build());
        return WebUtil.createRedirect(
            request,
            Routes.CASE_VIEW + "?id=" + created.getId(),
            MessageKeys.CONFIRMATION_CASE_CREATED);
      }

      if (ActionOperation.UPDATE.matchesParameter(operation)) {
        CaseDto updated = caseService.updateCase(actor, caseForm.getUpdateData());
        caseForm.setData(CasePageDto.builder().id(updated.getId()).item(updated).build());
      } else if (ActionOperation.ASSIGN.matchesParameter(operation)) {
        caseService.assignCase(actor, caseForm.getAssignData());
      } else if (ActionOperation.TRANSITION.matchesParameter(operation)) {
        caseService.transitionCase(actor, caseForm.getTransitionData());
      } else if (ActionOperation.COMMENT.matchesParameter(operation)) {
        caseService.addCaseComment(actor, caseForm.getCommentData());
      } else if (ActionOperation.ARCHIVE.matchesParameter(operation)) {
        caseService.archiveCase(actor, caseForm.getRecordData());
        return WebUtil.createRedirect(
            request, Routes.CASES, MessageKeys.CONFIRMATION_CASE_ARCHIVED);
      } else {
        throw new IllegalStateException(MessageKeys.BUSINESS_UNKNOWN_ACTION);
      }

      return WebUtil.createRedirect(
          request,
          Routes.CASE_VIEW + "?id=" + FormDataUtil.parseRecordId(caseForm.getId()),
          MessageKeys.CONFIRMATION_CASE_UPDATED);
    } catch (BusinessValidationException | ConcurrentUpdateException exception) {
      WebUtil.addBusinessError(request, exception);
      return new ActionForward(mapping.getInput());
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    }
  }
}
