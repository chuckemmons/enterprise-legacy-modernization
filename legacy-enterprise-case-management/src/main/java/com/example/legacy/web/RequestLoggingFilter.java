package com.example.legacy.web;

import com.example.legacy.observability.Telemetry;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Establishes request correlation before security, Actions, JSPs, and container error handling.
 *
 * @author Chuck Emmons
 */
public final class RequestLoggingFilter implements Filter {
  private boolean trustRequestId;

  /**
   * Uses application logger configuration without starting background resources.
   *
   * @param config the servlet filter configuration
   */
  @Override
  public void init(FilterConfig config) {
    trustRequestId = "true".equals(config.getInitParameter("trustRequestId"));
  }

  /**
   * Adds a correlation header and logs uncaught failures at the HTTP boundary.
   *
   * @param input the incoming servlet request
   * @param output the outgoing servlet response
   * @param chain the remaining servlet filter chain
   * @throws IOException if an I/O operation fails
   * @throws ServletException with a safe message if downstream request processing fails
   */
  @Override
  public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) input;
    HttpServletResponse response = (HttpServletResponse) output;
    String incoming = request.getHeader(HttpConstants.REQUEST_ID_HEADER);
    if (trustRequestId && Telemetry.validId(incoming)) {
      request.setAttribute(WebKeys.REQUEST_ID, incoming);
    }
    String requestId = RequestLoggingUtil.getOrCreateRequestId(request);
    response.setHeader(HttpConstants.REQUEST_ID_HEADER, requestId);
    response.setHeader("X-Implementation", "legacy");
    Telemetry.begin(requestId, request.getServletPath());
    long start = System.nanoTime();
    boolean failed = false;
    try {
      chain.doFilter(input, output);
    } catch (IOException | ServletException | RuntimeException | Error failure) {
      failed = true;
      RequestLoggingUtil.logUnexpectedFailure(request, failure);
      // Container exception logging must not receive credential-bearing messages or causes.
      throw new ServletException("Unexpected request failure; requestId=" + requestId);
    } finally {
      try {
        Telemetry.request(
            start,
            failed ? 500 : response.getStatus(),
            failed || Boolean.TRUE.equals(request.getAttribute(WebKeys.ERROR_LOGGED)));
      } finally {
        Telemetry.end();
      }
    }
  }

  /** Releases no resources because correlation state belongs to each request. */
  @Override
  public void destroy() {}
}
