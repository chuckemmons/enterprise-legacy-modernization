package com.example.legacy.web;

import com.example.legacy.service.exception.AccessDeniedException;
import com.example.legacy.service.exception.ApplicationException;
import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.MessageArgument;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.util.MessagesUtil;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.ResponseUtils;

/**
 * Shared servlet-facing helpers. Most business rules deliberately live below Struts.
 *
 * @author Chuck Emmons
 */
public final class WebUtil {
  private static final java.security.SecureRandom csrfRandom = new java.security.SecureRandom();

  /** Prevents instantiation of this static utility class. */
  private WebUtil() {}

  /**
   * Returns the signed-in username from an existing session or throws AccessDeniedException when no
   * actor is present.
   *
   * @param request the current HTTP request
   * @return the signed-in username from an existing session or throws AccessDeniedException when no
   *     actor is present
   * @throws AccessDeniedException if no signed-in actor is present in the session
   * @throws NullPointerException if {@code request} is {@code null}
   */
  public static String getAuthenticatedUsername(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute(WebKeys.ACTOR) == null) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_PLEASE_SIGN_IN);
    }
    return (String) session.getAttribute(WebKeys.ACTOR);
  }

  /**
   * Creates a fresh unpredictable CSRF token for a new authenticated or anonymous session.
   *
   * @return a new unpredictable CSRF token
   */
  public static String generateCsrfToken() {
    byte[] token = new byte[32];
    csrfRandom.nextBytes(token);
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token);
  }

  /**
   * Validates a multipart form token against the session token before any upload processing.
   *
   * @param submitted the submitted form value
   * @param expected the expected value
   * @throws AccessDeniedException if the submitted upload token does not match the session token
   */
  public static void validateCsrfToken(String submitted, Object expected) {
    if (submitted == null
        || expected == null
        || !java.security.MessageDigest.isEqual(
            submitted.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            expected.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      throw new AccessDeniedException(MessageKeys.BUSINESS_INVALID_UPLOAD_TOKEN);
    }
  }

  /**
   * Parses a nonlenient yyyy-MM-dd date in the server time zone; blank input returns {@code null}
   * and parse failures raise BusinessValidationException.
   *
   * @param date the date text to enter
   * @return the parsed date, or {@code null} for blank input
   * @throws BusinessValidationException if nonblank input is not a valid date
   */
  public static Date parseDate(String date) {
    if (date == null || date.trim().isEmpty()) {
      return null;
    }
    try {
      SimpleDateFormat dateFormat =
          new SimpleDateFormat(com.example.legacy.util.DatesUtil.INPUT_PATTERN);
      dateFormat.setLenient(false);
      return dateFormat.parse(date.trim());
    } catch (ParseException failure) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_DATE_MUST_BE_YYYY_MM_DD);
    }
  }

  /**
   * Formats a date as yyyy-MM-dd in the server time zone, returning an empty string for {@code
   * null}.
   *
   * @param date the date to format
   * @return the formatted date, or an empty string for {@code null}
   */
  public static String formatDate(Date date) {
    return date == null
        ? ""
        : new SimpleDateFormat(com.example.legacy.util.DatesUtil.INPUT_PATTERN).format(date);
  }

  /**
   * Builds an internal forward to a server-selected Tiles page definition.
   *
   * @param page the page view to render
   * @return an internal forward to the requested Tiles definition
   * @throws NullPointerException if {@code page} is {@code null}
   */
  public static ActionForward createViewForward(PageView page) {
    return new ActionForward(page.getDefinition());
  }

  /**
   * Builds a redirect to an application path with a UTF-8 URL-encoded confirmation message,
   * preserving any existing query string.
   *
   * @param request the current HTTP request
   * @param path the application-relative path
   * @param messageKey the message resource key
   * @return a redirect containing the encoded confirmation message
   * @throws IllegalStateException if the redirect path cannot be URL-encoded
   * @throws NullPointerException if {@code path} is {@code null}
   */
  public static ActionForward createRedirect(
      HttpServletRequest request, String path, String messageKey) {
    String extra = path.contains("?") ? "&" : "?";
    try {
      return new ActionForward(
          path
              + extra
              + WebKeys.MESSAGE
              + "="
              + URLEncoder.encode(
                  MessagesUtil.formatMessage(resolveLocale(request), messageKey),
                  java.nio.charset.StandardCharsets.UTF_8.name()),
          true);
    } catch (Exception failure) {
      throw new IllegalStateException(failure);
    }
  }

  /**
   * Presents expected application failures and logs unexpected exceptions once with request
   * context.
   *
   * @param request the current HTTP request
   * @param failure the failure to report
   * @return the forward to the application error page
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  public static ActionForward createFailureForward(HttpServletRequest request, Exception failure) {
    if (failure instanceof ApplicationException) {
      addBusinessError(request, (ApplicationException) failure);
    } else {
      RequestLoggingUtil.logUnexpectedFailure(request, failure);
      ActionErrors errors = new ActionErrors();
      errors.add(
          ActionMessages.GLOBAL_MESSAGE, new ActionMessage(MessageKeys.VALIDATION_UNEXPECTED));
      request.setAttribute(Globals.ERROR_KEY, errors);
    }
    return createViewForward(PageView.ERROR);
  }

  /**
   * Localizes expected application failures and escapes string arguments before html:errors renders
   * them.
   *
   * @param request the current HTTP request
   * @param failure the failure to report
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws NullPointerException if {@code request} or {@code failure} is {@code null}
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  public static void addBusinessError(HttpServletRequest request, ApplicationException failure) {
    Object[] arguments = failure.getArguments();
    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i] instanceof MessageArgument) {
        arguments[i] =
            MessagesUtil.formatMessage(
                resolveLocale(request), ((MessageArgument) arguments[i]).getKey());
      }
      if (arguments[i] instanceof String) {
        arguments[i] = ResponseUtils.filter((String) arguments[i]);
      }
    }
    ActionErrors errors = new ActionErrors();
    errors.add(
        ActionMessages.GLOBAL_MESSAGE, new ActionMessage(failure.getMessageKey(), arguments));
    request.setAttribute(Globals.ERROR_KEY, errors);
  }

  /**
   * Uses the Struts session locale when present, otherwise the request locale with an English
   * fallback.
   *
   * @param request the current HTTP request
   * @return the session or request locale, falling back to English
   * @throws NullPointerException if {@code request} is {@code null}
   */
  public static Locale resolveLocale(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object configured = session == null ? null : session.getAttribute(Globals.LOCALE_KEY);
    if (configured instanceof Locale) {
      return (Locale) configured;
    }
    return request.getLocale() == null ? Locale.ENGLISH : request.getLocale();
  }
}
