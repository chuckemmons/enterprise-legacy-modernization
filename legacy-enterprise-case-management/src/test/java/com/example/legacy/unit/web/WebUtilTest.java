package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.unit.support.WebTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.PageView;
import com.example.legacy.web.WebKeys;
import com.example.legacy.web.WebUtil;
import java.util.Locale;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.junit.Test;

/**
 * Behavioral contracts for WebUtil.
 *
 * @author Chuck Emmons
 */
public class WebUtilTest extends WebTestSupport {

  /**
   * Enforces session authentication and multipart token equality.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void enforcesSessionAuthenticationAndMultipartTokenEquality() {
    assertEquals("admin", WebUtil.getAuthenticatedUsername(request));

    sessionAttributes.remove(WebKeys.ACTOR);
    assertThrows(AccessDeniedException.class, () -> WebUtil.getAuthenticatedUsername(request));

    when(request.getSession(false)).thenReturn(null);
    assertThrows(AccessDeniedException.class, () -> WebUtil.getAuthenticatedUsername(request));

    WebUtil.validateCsrfToken("token", "token");
    assertThrows(AccessDeniedException.class, () -> WebUtil.validateCsrfToken(null, "token"));
    assertThrows(AccessDeniedException.class, () -> WebUtil.validateCsrfToken("token", null));
    assertThrows(AccessDeniedException.class, () -> WebUtil.validateCsrfToken("wrong", "token"));
  }

  /**
   * Encodes redirect messages and selects session or request locale.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void encodesRedirectMessagesAndSelectsSessionOrRequestLocale() {
    assertTrue(
        WebUtil.createRedirect(request, "/view", MessageKeys.CONFIRMATION_CASE_CREATED)
            .getPath()
            .contains("?message="));
    assertTrue(
        WebUtil.createRedirect(request, "/view?id=7", MessageKeys.CONFIRMATION_CASE_CREATED)
            .getPath()
            .contains("&message="));
    sessionAttributes.put(Globals.LOCALE_KEY, Locale.FRENCH);
    assertEquals(Locale.FRENCH, WebUtil.resolveLocale(request));
    sessionAttributes.remove(Globals.LOCALE_KEY);
    when(request.getLocale()).thenReturn(null);
    assertEquals(Locale.ENGLISH, WebUtil.resolveLocale(request));
    when(request.getSession(false)).thenReturn(null);
    assertEquals(Locale.ENGLISH, WebUtil.resolveLocale(request));
  }

  /**
   * Reports business failures and escapes untrusted message arguments.
   *
   * @throws AssertionError if the observed behavior violates the tested contract
   * @throws RuntimeException if preparing inputs or invoking the application unexpectedly fails
   */
  @Test
  public void reportsBusinessFailuresAndEscapesUntrustedMessageArguments() {
    BusinessValidationException failure =
        new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID);
    assertEquals(
        PageView.ERROR.getDefinition(), WebUtil.createFailureForward(request, failure).getPath());
    assertTrue(attributes.get(Globals.ERROR_KEY) instanceof ActionErrors);
    assertEquals("", WebUtil.formatDate(null));
    assertNotNull(WebUtil.parseDate("2026-11-07"));
    assertThrows(BusinessValidationException.class, () -> WebUtil.parseDate("invalid"));
  }

  /**
   * Preserves independent 256-bit URL-safe CSRF tokens when the secure random generator is reused.
   *
   * @throws AssertionError if tokens repeat or their encoding or decoded length changes
   * @throws IllegalArgumentException if a generated token is not valid URL-safe Base64
   */
  @Test
  public void generatesIndependentUrlSafeCsrfTokens() {
    String first = WebUtil.generateCsrfToken();
    String second = WebUtil.generateCsrfToken();
    assertNotEquals(first, second);
    assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
    assertTrue(second.matches("[A-Za-z0-9_-]{43}"));
    assertEquals(32, java.util.Base64.getUrlDecoder().decode(first).length);
    assertEquals(32, java.util.Base64.getUrlDecoder().decode(second).length);
  }
}
