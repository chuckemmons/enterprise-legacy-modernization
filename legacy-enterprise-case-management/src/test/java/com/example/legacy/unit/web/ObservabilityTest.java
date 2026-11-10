package com.example.legacy.unit.web;

import com.example.legacy.observability.Telemetry;
import com.example.legacy.service.transaction.HibernateTransactionTemplate;
import com.example.legacy.support.ServletTestSupport;
import com.example.legacy.web.HealthServlet;
import com.example.legacy.web.OperationsServlet;
import com.example.legacy.web.RequestLoggingFilter;
import com.example.legacy.web.WebKeys;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Checks trust boundaries, bounded labels, cleanup, probe failure and administrator access.
 *
 * @author Chuck Emmons
 */
public class ObservabilityTest {
  /**
   * Verifies opt-in upstream UUID trust, unsafe-header rejection and pooled-thread cleanup.
   *
   * @throws Exception if filter initialization or request execution fails
   * @throws AssertionError if correlation, cleanup or bounded-label expectations are violated
   */
  @Test
  public void trustsOnlyConfiguredCanonicalIdsAndClearsThreadContext() throws Exception {
    String id = UUID.randomUUID().toString();
    HttpServletRequest request = ServletTestSupport.createRequest(null, Collections.emptyMap());
    when(request.getHeader("X-Request-ID")).thenReturn(id);

    HttpServletResponse response = mock(HttpServletResponse.class);
    new RequestLoggingFilter().doFilter(request, response, mock(FilterChain.class));
    verify(response, never()).setHeader("X-Request-ID", id);

    request = ServletTestSupport.createRequest(null, Collections.emptyMap());
    when(request.getHeader("X-Request-ID")).thenReturn(id);

    FilterConfig config = mock(FilterConfig.class);
    when(config.getInitParameter("trustRequestId")).thenReturn("true");

    RequestLoggingFilter filter = new RequestLoggingFilter();
    filter.init(config);
    filter.doFilter(request, response, mock(FilterChain.class));

    verify(response).setHeader("X-Request-ID", id);

    Telemetry.transaction(System.nanoTime(), true);
    assertTrue(Telemetry.snapshot().contains("\"requestId\":\"background\""));

    when(request.getHeader("X-Request-ID")).thenReturn("password=secret\r\nInjected: yes");
    request.removeAttribute(WebKeys.REQUEST_ID);
    filter.doFilter(request, response, mock(FilterChain.class));

    assertFalse(Telemetry.snapshot().contains("Injected"));
    assertFalse(Telemetry.metrics().contains(id));
    assertEquals("other", Telemetry.operation("/password=secret"));
  }

  /**
   * Checks that lifecycle metrics include acquisition failures without retaining exception text.
   *
   * @throws AssertionError if failure handling, lifecycle interactions or histogram output differs
   */
  @Test
  public void metricsIncludeSessionAcquisitionAndFailureWithoutExceptionMessages() {
    SessionFactory factory = mock(SessionFactory.class);
    when(factory.openSession()).thenThrow(new IllegalStateException("SecretPassword!"));

    try {
      new HibernateTransactionTemplate(factory).execute(session -> null);
      fail("Expected original service failure");
    } catch (IllegalStateException expected) {
      assertEquals("SecretPassword!", expected.getMessage());
    }

    assertTrue(
        Telemetry.metrics()
            .contains(
                "kind=\"transaction\",implementation=\"legacy\",operation=\"background\",outcome=\"error\""));
    assertFalse(Telemetry.snapshot().contains("SecretPassword!"));

    Session session = mock(Session.class);
    Transaction transaction = mock(Transaction.class);
    doReturn(session).when(factory).openSession();
    when(session.beginTransaction()).thenReturn(transaction);

    new HibernateTransactionTemplate(factory).execute(active -> null);
    verify(transaction).commit();
    verify(session).close();

    assertTrue(Telemetry.metrics().contains("legacy_duration_seconds_bucket{"));
  }

  /**
   * Confirms history eviction and excludes exception messages and suppressed failure text.
   *
   * @throws AssertionError if event retention or identifier validation violates the contract
   */
  @Test
  public void retainsBoundedHistoryAndNeverSerializesThrowableText() {
    RuntimeException failure = new RuntimeException("PasswordCanary!");
    failure.addSuppressed(new RuntimeException("SuppressedCanary!"));
    Telemetry.failure(failure);

    assertFalse(Telemetry.snapshot().contains("PasswordCanary!"));
    assertFalse(Telemetry.snapshot().contains("SuppressedCanary!"));

    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Telemetry.class.getName());
    boolean parent = logger.getUseParentHandlers();
    logger.setUseParentHandlers(false);

    try {
      for (int i = 0; i < 1005; i++) {
        Telemetry.transaction(System.nanoTime(), true);
      }
    } finally {
      logger.setUseParentHandlers(parent);
    }

    assertEquals(1000, Telemetry.snapshot().split("\"timestamp\"", -1).length - 1);
    assertTrue(Telemetry.validId(UUID.randomUUID().toString()));
    assertFalse(Telemetry.validId("not-an-id"));
  }

  /**
   * Checks database-independent liveness and minimal failed-readiness responses and events.
   *
   * @throws Exception if servlet invocation or response writing fails
   * @throws AssertionError if a probe returns unexpected data or exposes the credential canary
   */
  @Test
  public void probesReturnMinimalUpAndDownResponses() throws Exception {
    HibernateTransactionTemplate transactions = mock(HibernateTransactionTemplate.class);
    HealthServlet servlet = new HealthServlet(transactions);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getPathInfo()).thenReturn("/live");

    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter output = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(output));

    servlet.service(request, response);

    verifyNoInteractions(transactions);
    assertTrue(output.toString().contains("\"status\":\"up\""));

    output.getBuffer().setLength(0);

    when(request.getPathInfo()).thenReturn("/ready");
    when(transactions.execute(any()))
        .thenThrow(new IllegalStateException("DatabasePasswordCanary!"));
    servlet.service(request, response);

    verify(response).setStatus(503);
    assertFalse(output.toString().contains("Canary"));
    assertFalse(Telemetry.snapshot().contains("DatabasePasswordCanary!"));
  }

  /**
   * Verifies administrator enforcement and histogram export through the operations servlet.
   *
   * @throws Exception if servlet invocation or response writing fails
   * @throws AssertionError if authorization or metric formatting differs from expectations
   */
  @Test
  public void operationsRequireAdministratorAndExportSafeMetrics() throws Exception {
    OperationsServlet servlet = new OperationsServlet();
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");

    HttpServletResponse response = mock(HttpServletResponse.class);
    servlet.service(request, response);
    verify(response).sendError(403);

    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
    when(session.getAttribute(WebKeys.ROLE)).thenReturn("ADMIN");
    when(request.getParameter("view")).thenReturn("metrics");

    StringWriter output = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(output));
    servlet.service(request, response);

    assertTrue(output.toString().contains("# TYPE legacy_duration_seconds histogram"));
  }

  /**
   * Keeps response I/O failures correlated without exposing their messages to container logs.
   *
   * @throws Exception if servlet or filter invocation fails unexpectedly
   * @throws AssertionError if a secret or cause escapes the request boundary
   */
  @Test
  public void servletIoFailuresAreLoggedOnceAndSanitized() throws Exception {
    for (javax.servlet.http.HttpServlet servlet :
        new javax.servlet.http.HttpServlet[] {new HealthServlet(), new OperationsServlet()}) {
      HttpServletRequest request = ServletTestSupport.createRequest(null, Collections.emptyMap());
      when(request.getMethod()).thenReturn("GET");
      when(request.getPathInfo()).thenReturn("/live");

      HttpSession session = mock(HttpSession.class);
      when(request.getSession(false)).thenReturn(session);
      when(session.getAttribute(WebKeys.ROLE)).thenReturn("ADMIN");
      doReturn("events").when(request).getParameter("view");

      HttpServletResponse response = mock(HttpServletResponse.class);
      when(response.getWriter()).thenThrow(new java.io.IOException("IoSecretCanary"));

      String requestId = UUID.randomUUID().toString();
      request.setAttribute(WebKeys.REQUEST_ID, requestId);

      new RequestLoggingFilter()
          .doFilter(request, response, (input, output) -> servlet.service(input, output));
      verify(response).resetBuffer();
      verify(response).setStatus(500);

      String snapshot = Telemetry.snapshot();
      assertFalse(snapshot.contains("IoSecretCanary"));
      String failure = "\"requestId\":\"" + requestId + "\"";
      assertEquals(2, snapshot.split(java.util.regex.Pattern.quote(failure), -1).length - 1);
      assertEquals(Boolean.TRUE, request.getAttribute(WebKeys.ERROR_LOGGED));
    }
  }

  /**
   * Verifies that the JSON formatter emits telemetry records and drops other logger categories.
   *
   * @throws AssertionError if unrelated messages are forwarded or JSON line formatting differs
   */
  @Test
  public void jsonLineFormatterDoesNotForwardThirdPartyMessages() {
    com.example.legacy.observability.JsonLineFormatter formatter =
        new com.example.legacy.observability.JsonLineFormatter();
    java.util.logging.LogRecord record =
        new java.util.logging.LogRecord(java.util.logging.Level.INFO, "SecretFromAnotherLogger");
    record.setLoggerName("third.party");
    assertEquals("", formatter.format(record));

    record.setLoggerName(Telemetry.class.getName());
    record.setMessage("{\"event\":\"check\"}");
    assertEquals("{\"event\":\"check\"}\n", formatter.format(record));
  }
}
