package com.example.legacy.web;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Correlates operational failures without logging credentials, submitted bodies, or query strings.
 *
 * @author Chuck Emmons
 */
public final class RequestLoggingUtil {
  private static final Logger LOG = Logger.getLogger(RequestLoggingUtil.class.getName());

  /** Prevents instantiation of this shared request logging helper. */
  private RequestLoggingUtil() {}

  /**
   * Creates a trusted server-generated correlation identifier once per original request.
   *
   * @param request the current HTTP request
   * @return the trusted correlation identifier associated with the request
   * @throws NullPointerException if {@code request} is {@code null}
   */
  public static String getOrCreateRequestId(HttpServletRequest request) {
    Object existing = request.getAttribute(WebKeys.REQUEST_ID);
    if (existing == null) {
      existing = UUID.randomUUID().toString();
      request.setAttribute(WebKeys.REQUEST_ID, existing);
    }
    return existing.toString();
  }

  /**
   * Logs an unexpected failure once, preserving the full cause and safe request context.
   *
   * @param request the current HTTP request
   * @param failure the failure to report
   * @throws NullPointerException if {@code request} is {@code null}
   */
  public static void logUnexpectedFailure(HttpServletRequest request, Throwable failure) {
    if (Boolean.TRUE.equals(request.getAttribute(WebKeys.ERROR_LOGGED))) {
      return;
    }
    request.setAttribute(WebKeys.ERROR_LOGGED, Boolean.TRUE);
    String path = String.valueOf(request.getServletPath()).replace('\r', '_').replace('\n', '_');
    Object record = request.getAttribute(WebKeys.RECORD_ID);
    LOG.log(
        Level.SEVERE,
        "requestId=" + getOrCreateRequestId(request) + " operation=" + path + " recordId=" + record,
        failure);
  }
}
