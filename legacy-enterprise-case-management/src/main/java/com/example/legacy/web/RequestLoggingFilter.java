package com.example.legacy.web;

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
  /**
   * Uses application logger configuration without starting background resources.
   *
   * @param config the servlet filter configuration
   */
  @Override
  public void init(FilterConfig config) {}

  /**
   * Adds a correlation header and logs uncaught failures at the HTTP boundary.
   *
   * @param input the incoming servlet request
   * @param output the outgoing servlet response
   * @param chain the remaining servlet filter chain
   * @throws IOException if an I/O operation fails
   * @throws ServletException if servlet initialization or request processing fails
   * @throws Error if the downstream filter or servlet raises an error
   * @throws RuntimeException if the downstream filter or servlet fails while handling the request
   */
  @Override
  public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) input;
    ((HttpServletResponse) output)
        .setHeader(
            HttpConstants.REQUEST_ID_HEADER, RequestLoggingUtil.getOrCreateRequestId(request));
    try {
      chain.doFilter(input, output);
    } catch (IOException | ServletException | RuntimeException | Error failure) {
      RequestLoggingUtil.logUnexpectedFailure(request, failure);
      throw failure;
    }
  }

  /** Releases no resources because correlation state belongs to each request. */
  @Override
  public void destroy() {}
}
