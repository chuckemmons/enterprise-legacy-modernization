package com.example.legacy.web;

import com.example.legacy.domain.Role;
import com.example.legacy.observability.Telemetry;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves the read-only administrator dashboard and operational exports.
 *
 * <p>The security filter refreshes active-account status and role before every call.
 *
 * @author Chuck Emmons
 */
public final class OperationsServlet extends HttpServlet {
  /**
   * Requires an administrator and serves the dashboard, JSON event snapshot or text histograms.
   *
   * @param request the authenticated request and optional {@code view} selection
   * @param response the response receiving the selected operational representation
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      if (request.getSession(false) == null
          || !Role.ADMIN.matchesCode(
              (String) request.getSession(false).getAttribute(WebKeys.ROLE))) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
      }
      String view = request.getParameter("view");
      response.setCharacterEncoding("UTF-8");
      response.setHeader("Cache-Control", "no-store");
      if ("metrics".equals(view)) {
        response.setContentType("text/plain; version=0.0.4; charset=utf-8");
        response.getWriter().write(Telemetry.metrics());
      } else if ("events".equals(view)) {
        response.setContentType("application/json");
        response.getWriter().write(Telemetry.snapshot());
      } else if (view == null) {
        response.setContentType("text/html");
        try (InputStream page =
            getServletContext().getResourceAsStream("/WEB-INF/views/operations.html")) {
          if (page == null) {
            throw new IOException("Dashboard resource unavailable");
          }
          byte[] buffer = new byte[4096];
          int count;
          while ((count = page.read(buffer)) != -1) {
            response.getOutputStream().write(buffer, 0, count);
          }
        }
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } catch (IOException failure) {
      RequestLoggingUtil.logUnexpectedFailure(request, failure);
      if (!response.isCommitted()) {
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }
}
