package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.legacy.service.AccountAccessService;
import com.example.legacy.service.dto.AccountDto;
import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.web.HttpConstants;
import com.example.legacy.web.Routes;
import com.example.legacy.web.SecurityFilter;
import com.example.legacy.web.WebKeys;
import org.junit.Before;
import org.junit.Test;

/**
 * Behavioral contracts for SecurityFilter.
 *
 * @author Chuck Emmons
 */
public class SecurityFilterTest extends WebTestSupport {

  private AccountAccessService access;
  private SecurityFilter filter;
  private javax.servlet.FilterChain chain;

  /** Creates isolated collaborators and input state for each test. */
  @Before
  public void createFilter() {
    access = mock(AccountAccessService.class);
    filter = new SecurityFilter(access);
    chain = mock(javax.servlet.FilterChain.class);
    when(access.loadActiveAccount("admin"))
        .thenReturn(AccountDto.builder().role("ADMIN").displayName("Administrator").build());
  }

  /**
   * Generates tokens and allows public login without database access.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void generatesTokensAndAllowsPublicLoginWithoutDatabaseAccess() throws Exception {
    sessionAttributes.clear();
    when(request.getServletPath()).thenReturn(Routes.LOGIN);
    when(request.getMethod()).thenReturn("GET");
    filter.doFilter(request, response, chain);
    assertNotNull(sessionAttributes.get(WebKeys.CSRF));
    verifyNoInteractions(access);
    verify(chain).doFilter(request, response);
    verify(response).setHeader(HttpConstants.CONTENT_TYPE_OPTIONS, HttpConstants.NO_SNIFF);
    verify(response).setHeader(HttpConstants.FRAME_OPTIONS, HttpConstants.DENY);
    verify(response).setHeader(HttpConstants.CACHE_CONTROL, HttpConstants.NO_STORE);
    filter.init(null);
    filter.destroy();
  }

  /**
   * Redirects anonymous requests before loading an account.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void redirectsAnonymousRequestsBeforeLoadingAnAccount() throws Exception {
    sessionAttributes.remove(WebKeys.ACTOR);
    filter.doFilter(request, response, chain);
    verify(response).sendRedirect("/cases" + Routes.LOGIN);
    verifyNoInteractions(access, chain);
  }

  /**
   * Refreshes the role and allows validated post requests.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void refreshesTheRoleAndAllowsValidatedPostRequests() throws Exception {
    parameters.put(WebKeys.CSRF, "valid-token");
    when(request.getServletPath()).thenReturn(Routes.CASE_CREATE);
    filter.doFilter(request, response, chain);
    assertEquals("ADMIN", sessionAttributes.get(WebKeys.ROLE));
    assertEquals("Administrator", sessionAttributes.get(WebKeys.DISPLAY_NAME));
    verify(chain).doFilter(request, response);
  }

  /**
   * Invalidates unavailable accounts and redirects to login.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void invalidatesUnavailableAccountsAndRedirectsToLogin() throws Exception {
    when(access.loadActiveAccount("admin")).thenThrow(new AccessDeniedException("missing"));
    filter.doFilter(request, response, chain);
    verify(session).invalidate();
    verify(response).sendRedirect("/cases" + Routes.LOGIN);
    verifyNoInteractions(chain);
  }

  /**
   * Rejects missing and mismatched csrf tokens.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void rejectsMissingAndMismatchedCsrfTokens() throws Exception {
    filter.doFilter(request, response, chain);
    parameters.put(WebKeys.CSRF, "wrong-token");
    filter.doFilter(request, response, chain);
    verify(response, times(2)).sendError(eq(403), anyString());
    verifyNoInteractions(chain);
  }

  /**
   * Rejects missing original tokens even when the session cannot retain one.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void rejectsMissingOriginalTokensEvenWhenTheSessionCannotRetainOne() throws Exception {
    when(session.getAttribute(WebKeys.CSRF)).thenReturn(null);
    parameters.put(WebKeys.CSRF, "submitted");
    filter.doFilter(request, response, chain);
    verify(response).sendError(eq(403), anyString());
    verifyNoInteractions(chain);
  }

  /**
   * Leaves multipart token validation to the upload action.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void leavesMultipartTokenValidationToTheUploadAction() throws Exception {
    when(request.getServletPath()).thenReturn(Routes.CASE_UPLOAD);
    filter.doFilter(request, response, chain);
    verify(chain).doFilter(request, response);
  }

  /**
   * Rejects get for every mutation route.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void rejectsGetForEveryMutationRoute() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    for (String route :
        new String[] {
          Routes.AUTHENTICATE,
          Routes.LOGOUT,
          Routes.CASE_CREATE,
          Routes.CASE_UPDATE,
          Routes.CASE_ASSIGN,
          Routes.CASE_TRANSITION,
          Routes.CASE_COMMENT,
          Routes.CASE_ARCHIVE,
          Routes.CASE_UPLOAD,
          Routes.USER_CREATE,
          Routes.USER_ROLE,
          Routes.USER_DISABLE,
          Routes.NOTIFICATION_READ
        }) {
      when(request.getServletPath()).thenReturn(route);
      filter.doFilter(request, response, chain);
    }
    verify(response, times(13)).sendError(eq(405), anyString());
    verifyNoInteractions(chain);
  }

  /**
   * Permits read only get and validated public authentication.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void permitsReadOnlyGetAndValidatedPublicAuthentication() throws Exception {
    when(request.getMethod()).thenReturn("GET");
    when(request.getServletPath()).thenReturn(Routes.CASES);
    filter.doFilter(request, response, chain);
    when(request.getMethod()).thenReturn("post");
    when(request.getServletPath()).thenReturn(Routes.AUTHENTICATE);
    parameters.put(WebKeys.CSRF, "valid-token");
    filter.doFilter(request, response, chain);
    verify(chain, times(2)).doFilter(request, response);
  }
}
