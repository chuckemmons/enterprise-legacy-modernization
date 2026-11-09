package com.example.legacy.unit.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.legacy.web.WebKeys;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionMapping;
import org.junit.Before;

/**
 * Isolates HTTP boundaries while keeping request and session attributes observable.
 *
 * @author Chuck Emmons
 */
public abstract class WebTestSupport {
  /** Test fixture for request. */
  protected HttpServletRequest request;

  /** Test fixture for response. */
  protected HttpServletResponse response;

  /** Test fixture for session. */
  protected HttpSession session;

  /** Test fixture for attributes. */
  protected Map<String, Object> attributes;

  /** Test fixture for session attributes. */
  protected Map<String, Object> sessionAttributes;

  /** Test fixture for parameters. */
  protected Map<String, String> parameters;

  /** Test fixture for text. */
  protected StringWriter text;

  /** Test fixture for bytes. */
  protected ByteArrayOutputStream bytes;

  /**
   * Creates fresh servlet collaborators and captures response bytes without starting a container.
   *
   * @throws Exception if a mocked response stream cannot be configured
   */
  @Before
  public void createHttpCollaborators() throws Exception {
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    session = mock(HttpSession.class);
    attributes = new HashMap<>();
    sessionAttributes = new HashMap<>();
    parameters = new HashMap<>();
    sessionAttributes.put(WebKeys.ACTOR, "admin");
    sessionAttributes.put(WebKeys.CSRF, "valid-token");
    when(request.getAttribute(anyString()))
        .thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
    doAnswer(
            invocation -> {
              attributes.put(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(request)
        .setAttribute(anyString(), any());
    doAnswer(
            invocation -> {
              attributes.remove(invocation.getArgument(0));
              return null;
            })
        .when(request)
        .removeAttribute(anyString());
    when(request.getParameter(anyString()))
        .thenAnswer(invocation -> parameters.get(invocation.getArgument(0)));
    when(request.getSession()).thenReturn(session);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(session.getAttribute(anyString()))
        .thenAnswer(invocation -> sessionAttributes.get(invocation.getArgument(0)));
    doAnswer(
            invocation -> {
              sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(session)
        .setAttribute(anyString(), any());
    when(request.getLocale()).thenReturn(Locale.ENGLISH);
    when(request.getMethod()).thenReturn("POST");
    when(request.getServletPath()).thenReturn("/test.do");
    when(request.getContextPath()).thenReturn("/cases");
    text = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(text));
    bytes = new ByteArrayOutputStream();
    when(response.getOutputStream())
        .thenReturn(
            new ServletOutputStream() {

              /**
               * Captures one response byte for content assertions.
               *
               * @param value the byte to capture
               */
              @Override
              public void write(int value) {
                bytes.write(value);
              }

              /**
               * Reports readiness for synchronous response writes.
               *
               * @return {@code true} because the fixture writes to memory
               */
              @Override
              public boolean isReady() {
                return true;
              }

              /**
               * Accepts the unused listener for this synchronous response fixture.
               *
               * @param listener the asynchronous listener, unused by the fixture
               */
              @Override
              public void setWriteListener(javax.servlet.WriteListener listener) {}
            });
  }

  /**
   * Creates an operation mapping with a predictable correction page.
   *
   * @param operation the operation parameter
   * @return the configured mapping
   */
  protected ActionMapping mapping(String operation) {
    ActionMapping value = new ActionMapping();
    value.setParameter(operation);
    value.setInput("page.input");
    return value;
  }
}
