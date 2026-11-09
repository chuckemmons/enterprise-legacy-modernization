package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.AuthenticationService;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.dto.AuthDto;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.AuthAction;
import com.example.legacy.web.AuthForm;
import com.example.legacy.web.PageView;
import com.example.legacy.web.Routes;
import com.example.legacy.web.WebKeys;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for AuthAction.
 *
 * @author Chuck Emmons
 */
public class AuthActionTest extends WebTestSupport {

  private AuthenticationService service;
  private AuthAction action;
  private AuthForm form;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createAction() {
    service = mock(AuthenticationService.class);
    action = new AuthAction(service);
    form = new AuthForm();
    form.setUsername("worker");
    form.setPassword("SubmittedSecret");
  }

  /**
   * Shows login without authenticating and clears the secret.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void showsLoginWithoutAuthenticatingAndClearsTheSecret() {
    assertEquals(
        PageView.LOGIN.getDefinition(),
        action.execute(mapping("login"), form, request, response).getPath());
    assertNull(form.getPassword());
    verifyNoInteractions(service);
  }

  /**
   * Logs out whether or not a session exists.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void logsOutWithOrWithoutSession() {
    assertTrue(action.execute(mapping("logout"), form, request, response).getRedirect());
    verify(session).invalidate();
    when(request.getSession(false)).thenReturn(null);
    assertEquals(
        Routes.LOGIN, action.execute(mapping("logout"), form, request, response).getPath());
  }

  /**
   * Rotates the authenticated session and stores only public account data.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void rotatesTheAuthenticatedSessionAndStoresOnlyPublicAccountData() {
    javax.servlet.http.HttpSession previousSession = mock(javax.servlet.http.HttpSession.class);
    when(request.getSession(false)).thenReturn(previousSession);
    AccountDto account =
        AccountDto.builder().username("worker").role("WORKER").displayName("Worker").build();
    when(service.authenticate(any(AuthDto.class)))
        .thenReturn(AuthDto.builder().account(account).build());
    ActionForward forward = action.execute(mapping("authenticate"), form, request, response);
    assertEquals(Routes.DASHBOARD, forward.getPath());
    assertTrue(forward.getRedirect());
    verify(previousSession).invalidate();
    assertEquals("worker", sessionAttributes.get(WebKeys.ACTOR));
    assertEquals("WORKER", sessionAttributes.get(WebKeys.ROLE));
    assertEquals("Worker", sessionAttributes.get(WebKeys.DISPLAY_NAME));
    assertNotEquals("valid-token", sessionAttributes.get(WebKeys.CSRF));
    assertNull(form.getPassword());
  }

  /**
   * Authenticates without an old session and displays credential failures.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void authenticatesWithoutAnOldSessionAndDisplaysCredentialFailures() {
    when(request.getSession(false)).thenReturn(null);
    when(service.authenticate(any(AuthDto.class)))
        .thenReturn(
            AuthDto.builder().account(AccountDto.builder().username("worker").build()).build());
    assertEquals(
        Routes.DASHBOARD,
        action.execute(mapping("authenticate"), form, request, response).getPath());
    when(service.authenticate(any(AuthDto.class))).thenReturn(AuthDto.builder().build());
    assertEquals(
        PageView.LOGIN.getDefinition(),
        action.execute(mapping("authenticate"), form, request, response).getPath());
    assertTrue(attributes.get(Globals.ERROR_KEY) instanceof ActionErrors);
    assertNull(form.getPassword());
  }

  /**
   * Reports unexpected operations without retaining passwords.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reportsUnexpectedOperationsWithoutRetainingPasswords() {
    assertEquals(
        PageView.ERROR.getDefinition(),
        action.execute(mapping("unknown"), form, request, response).getPath());
    assertNull(form.getPassword());
  }
}
