package com.example.legacy.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.example.legacy.web.Routes;
import com.example.legacy.web.WebKeys;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.struts.Globals;
import org.apache.struts.config.ModuleConfig;

/**
 * Mockito servlet fixtures shared by independently compiled unit and framework-integration
 * packages.
 *
 * @author Chuck Emmons
 */
public final class ServletTestSupport {
  /** Prevents instantiation of test fixture helpers. */
  private ServletTestSupport() {}

  /**
   * Supplies request/session attributes without a servlet container or an authenticated database
   * call.
   *
   * @param module the Struts module configuration
   * @param parameters the request parameters indexed by name
   * @return the mocked HTTP request with the configured attributes and session
   * @throws NullPointerException if {@code parameters} is {@code null}
   */
  public static HttpServletRequest createRequest(
      ModuleConfig module, Map<String, String> parameters) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Globals.MODULE_KEY, module);
    attributes.put(WebKeys.RECORD_ID, 7L);
    Map<String, Object> sessionAttributes = new HashMap<>();
    sessionAttributes.put(WebKeys.ACTOR, "admin");
    HttpSession session =
        mock(HttpSession.class, withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
    when(session.getAttribute(anyString()))
        .thenAnswer(call -> sessionAttributes.get(call.getArgument(0)));
    doAnswer(
            call -> {
              sessionAttributes.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(session)
        .setAttribute(anyString(), any());
    HttpServletRequest request =
        mock(
            HttpServletRequest.class,
            withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
    when(request.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
    doAnswer(
            call -> {
              attributes.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(request)
        .setAttribute(anyString(), any());
    doAnswer(
            call -> {
              attributes.remove(call.getArgument(0));
              return null;
            })
        .when(request)
        .removeAttribute(anyString());
    when(request.getAttributeNames())
        .thenAnswer(call -> Collections.enumeration(attributes.keySet()));
    when(request.getSession()).thenReturn(session);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(request.getParameter(anyString()))
        .thenAnswer(call -> {
          String name = call.getArgument(0);
          if (!"page".equals(name) && !"pageNumber".equals(name)) {
            throw new AssertionError("Validation may only read legacy pagination parameters");
          }
          return parameters.get(name);
        });
    when(request.getContentType()).thenReturn(parameters.get("_contentType"));
    when(request.getLocale()).thenReturn(Locale.ENGLISH);
    when(request.getMethod()).thenReturn("POST");
    when(request.getServletPath()).thenReturn(Routes.CASE_CREATE);
    return request;
  }

  /**
   * Supplies a Mockito application context with stateful attributes and real configuration
   * resources.
   *
   * @param attributes the request attributes indexed by name
   * @return the servlet context
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  public static ServletContext createServletContext(Map<String, Object> attributes) {
    ServletContext context =
        mock(
            ServletContext.class,
            withSettings().strictness(org.mockito.quality.Strictness.LENIENT));
    when(context.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
    doAnswer(
            call -> {
              attributes.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(context)
        .setAttribute(anyString(), any());
    doAnswer(
            call -> {
              attributes.remove(call.getArgument(0));
              return null;
            })
        .when(context)
        .removeAttribute(anyString());
    try {
      when(context.getResource(anyString()))
          .thenAnswer(call -> {
            File file = new File("src/main/webapp" + call.getArgument(0));
            return file.isFile() ? file.toURI().toURL() : null;
          });
    } catch (java.net.MalformedURLException impossible) {
      throw new AssertionError(impossible);
    }
    when(context.getResourceAsStream(anyString()))
        .thenAnswer(call -> {
          File file = new File("src/main/webapp" + call.getArgument(0));
          return file.isFile() ? Files.newInputStream(file.toPath()) : null;
        });
    when(context.getInitParameterNames()).thenAnswer(call -> Collections.emptyEnumeration());
    return context;
  }
}
