package com.example.legacy.web;

import com.example.legacy.service.BackOfficeService;
import com.example.legacy.service.dto.UserPageDto;
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
 * Routes administrator account forms to services without exposing Hibernate entities.
 *
 * @author Chuck Emmons
 */
public final class UserAction extends Action {
  private final BackOfficeService backOfficeService;

  /** Creates this action and its reusable service collaborators. */
  public UserAction() {
    this(new BackOfficeService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param backOfficeService the back office service collaborator
   * @throws NullPointerException if {@code backOfficeService} is {@code null}
   */
  public UserAction(BackOfficeService backOfficeService) {
    this.backOfficeService =
        java.util.Objects.requireNonNull(backOfficeService, "backOfficeService");
  }

  /**
   * Converts submitted user fields to a DTO and returns service data through the user form.
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
    UserForm userForm = (UserForm) form;
    try {
      UserPageDto data = userForm.getData();
      String actor = WebUtil.getAuthenticatedUsername(request);
      String operation = mapping.getParameter();
      if (ActionOperation.LIST.matchesParameter(operation)) {
        userForm.setData(
            UserPageDto.builder(data)
                .users(backOfficeService.listAllAccounts(actor))
                .password(null)
                .build());
        return WebUtil.createViewForward(PageView.USERS);
      }
      if (ActionOperation.CREATE.matchesParameter(operation)) {
        backOfficeService.createUser(actor, userForm.getCreateData());
        data = UserPageDto.builder(data).password(null).build();
      } else if (ActionOperation.ROLE.matchesParameter(operation)) {
        backOfficeService.changeUserRole(actor, userForm.getRoleData());
      } else if (ActionOperation.DISABLE.matchesParameter(operation)) {
        backOfficeService.toggleUserEnabled(actor, userForm.getRecordData());
      } else {
        throw new IllegalStateException(MessageKeys.BUSINESS_UNKNOWN_ACTION);
      }
      userForm.setData(data);
      return WebUtil.createRedirect(request, Routes.USERS, MessageKeys.CONFIRMATION_USER_UPDATED);
    } catch (BusinessValidationException | ConcurrentUpdateException exception) {
      WebUtil.addBusinessError(request, exception);
      return new ActionForward(mapping.getInput());
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    } finally {
      userForm.setPassword(null);
    }
  }
}
