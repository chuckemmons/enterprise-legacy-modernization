package com.example.legacy.web;

import com.example.legacy.observability.Telemetry;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

/**
 * Correlates operational failures without logging credentials, submitted bodies, or query strings.
 *
 * @author Chuck Emmons
 */
public final class RequestLoggingUtil {

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
   * Logs an unexpected failure once, omitting exception messages and submitted data.
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
    Telemetry.failure(failure);
  }
}
