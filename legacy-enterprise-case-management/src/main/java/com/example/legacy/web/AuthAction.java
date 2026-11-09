package com.example.legacy.web;

import com.example.legacy.service.AuthenticationService;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.util.MessageKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

/**
 * Handles login/session transport using bound credentials and credential-free account DTOs.
 *
 * @author Chuck Emmons
 */
public final class AuthAction extends Action {
  private final AuthenticationService authenticationService;

  /** Creates this action and its reusable service collaborators. */
  public AuthAction() {
    this(new AuthenticationService());
  }

  /**
   * Accepts explicitly supplied application collaborators for request-processing tests.
   *
   * @param authenticationService the authentication service collaborator
   * @throws NullPointerException if {@code authenticationService} is {@code null}
   */
  public AuthAction(AuthenticationService authenticationService) {
    this.authenticationService =
        java.util.Objects.requireNonNull(authenticationService, "authenticationService");
  }

  /**
   * Delegates form credentials to authentication and manages only session, token, and navigation
   * state.
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
    AuthForm authForm = (AuthForm) form;
    try {
      AuthDto data = authForm.getData();

      String operation = mapping.getParameter();
      if (ActionOperation.LOGIN.matchesParameter(operation)) {
        authForm.setData(data);
        return WebUtil.createViewForward(PageView.LOGIN);
      }
      if (ActionOperation.LOGOUT.matchesParameter(operation)) {
        HttpSession session = request.getSession(false);
        if (session != null) {
          session.invalidate();
        }
        authForm.setData(data);
        return new ActionForward(Routes.LOGIN, true);
      }
      if (ActionOperation.AUTHENTICATE.matchesParameter(operation)) {
        authForm.setData(authenticationService.authenticate(data));
        AccountDto account = authForm.getAccount();
        if (account == null) {
          ActionErrors errors = new ActionErrors();
          errors.add(
              ActionMessages.GLOBAL_MESSAGE, new ActionMessage(MessageKeys.VALIDATION_CREDENTIALS));
          saveErrors(request, errors);
          return WebUtil.createViewForward(PageView.LOGIN);
        }
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
          oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(WebKeys.ACTOR, account.getUsername());
        session.setAttribute(WebKeys.ROLE, account.getRole());
        session.setAttribute(WebKeys.DISPLAY_NAME, account.getDisplayName());
        session.setAttribute(WebKeys.CSRF, WebUtil.generateCsrfToken());
        return new ActionForward(Routes.DASHBOARD, true);
      }
      throw new IllegalStateException(MessageKeys.BUSINESS_UNKNOWN_ACTION);
    } catch (Exception exception) {
      return WebUtil.createFailureForward(request, exception);
    } finally {
      authForm.setPassword(null);
    }
  }
}
