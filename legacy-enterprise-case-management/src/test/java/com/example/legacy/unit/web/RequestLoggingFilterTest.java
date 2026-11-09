package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.support.ServletTestSupport;
import com.example.legacy.util.MessageKeys;
import com.example.legacy.web.HttpConstants;
import com.example.legacy.web.RequestLoggingFilter;
import com.example.legacy.web.RequestLoggingUtil;
import com.example.legacy.web.WebUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests request correlation, safe operational context, log-once behavior, and escaped business
 * messages.
 *
 * @author Chuck Emmons
 */
public class RequestLoggingFilterTest {
  private final Logger logger = Logger.getLogger(RequestLoggingUtil.class.getName());
  private final List<LogRecord> records = new ArrayList<>();
  private Handler capture;
  private boolean parent;
  private HttpServletRequest request;

  /** Captures only this logger's records and creates an independent mocked HTTP request. */
  @Before
  public void setUp() {
    request = ServletTestSupport.createRequest(null, Collections.emptyMap());
    capture =
        new Handler() {
          /**
           * Captures the original failure and diagnostic context.
           *
           * @param record the log record to inspect
           */
          @Override
          public void publish(LogRecord record) {
            records.add(record);
          }

          /** Keeps no buffered output. */
          @Override
          public void flush() {}

          /** Owns no external resource. */
          @Override
          public void close() {}
        };
    parent = logger.getUseParentHandlers();
    logger.setUseParentHandlers(false);
    logger.addHandler(capture);
  }

  /** Restores logger configuration even when an assertion fails. */
  @After
  public void tearDown() {
    logger.removeHandler(capture);
    logger.setUseParentHandlers(parent);
  }

  /**
   * Adds a trusted correlation header and passes the original request through the filter.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void correlatesSuccessfulRequests() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    new RequestLoggingFilter().doFilter(request, response, chain);
    ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
    verify(response).setHeader(eq(HttpConstants.REQUEST_ID_HEADER), id.capture());
    UUID.fromString(id.getValue());
    assertEquals(RequestLoggingUtil.getOrCreateRequestId(request), id.getValue());
    verify(chain).doFilter(request, response);
    assertTrue(records.isEmpty());
  }

  /**
   * Logs unhandled failures once with the original cause and bounded request context.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void logsUnhandledFailureOnlyOnce() throws Exception {
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    ServletException expected = new ServletException("original failure");
    doThrow(expected).when(chain).doFilter(request, response);
    assertSame(
        expected,
        assertThrows(
            ServletException.class,
            () -> new RequestLoggingFilter().doFilter(request, response, chain)));
    RequestLoggingUtil.logUnexpectedFailure(request, expected);
    assertEquals(1, records.size());
    assertSame(expected, records.get(0).getThrown());
    assertTrue(records.get(0).getMessage().contains("operation=/caseCreate.do"));
    assertTrue(records.get(0).getMessage().contains("recordId=7"));
  }

  /**
   * Leaves business corrections out of error logs and escapes supplied formatting arguments.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  @Test
  public void escapesExpectedCorrectionsWithoutLogging() {
    WebUtil.addBusinessError(
        request,
        new BusinessValidationException(MessageKeys.VALIDATION_BUSINESS, "<script>value</script>"));
    ActionMessage error =
        (ActionMessage) ((ActionErrors) request.getAttribute(Globals.ERROR_KEY)).get().next();
    assertFalse(error.getValues()[0].toString().contains("<script>"));
    assertTrue(records.isEmpty());
  }
}
