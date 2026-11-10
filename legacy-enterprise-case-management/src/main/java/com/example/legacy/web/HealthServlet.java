package com.example.legacy.web;

import com.example.legacy.observability.Telemetry;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Serves minimal liveness and database readiness probes without exposing connection details.
 *
 * <p>Deployments must restrict {@code /health/*} to their monitoring network.
 *
 * @author Chuck Emmons
 */
public final class HealthServlet extends HttpServlet {
  private final HibernateTransactionTemplate transactions;

  /** Creates the container-managed probe servlet with lazy production database access. */
  public HealthServlet() {
    this(new HibernateTransactionTemplate());
  }

  /**
   * Accepts a transaction collaborator for isolated probe testing.
   *
   * @param transactions the transaction template used for database readiness queries
   */
  public HealthServlet(HibernateTransactionTemplate transactions) {
    this.transactions = transactions;
  }

  /**
   * Returns liveness without database access or readiness after a database query.
   *
   * <p>Readiness failures produce a minimal 503 response. Unknown probe paths return 404.
   *
   * @param request the HTTP request identifying the probe path
   * @param response the response receiving the minimal JSON result
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      response.setContentType("application/json");
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("X-Content-Type-Options", "nosniff");
      if ("/live".equals(request.getPathInfo())) {
        response.getWriter().write("{\"implementation\":\"legacy\",\"status\":\"up\"}");
      } else if ("/ready".equals(request.getPathInfo())) {
        long start = System.nanoTime();
        boolean healthy = false;
        try {
          healthy =
              transactions.execute(session -> {
                Object result = session.createSQLQuery("SELECT 1").setTimeout(3).uniqueResult();
                return result instanceof Number && ((Number) result).intValue() == 1;
              });
        } catch (RuntimeException failure) {
          Telemetry.failure(failure);
        }
        Telemetry.check(start, healthy);
        response.setStatus(healthy ? 200 : 503);
        response
            .getWriter()
            .write(
                "{\"implementation\":\"legacy\",\"status\":\"" + (healthy ? "up" : "down") + "\"}");
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
