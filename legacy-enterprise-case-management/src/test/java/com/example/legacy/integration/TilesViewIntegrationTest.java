package com.example.legacy.integration;

import static org.junit.Assert.assertTrue;

import com.example.legacy.web.WebUtil;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ControllerConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.PlugInConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.TilesPlugin;
import org.apache.struts.tiles.TilesRequestProcessor;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Exercises the configured Tiles plug-in, inherited definitions, and Struts forwarding without a
 * database.
 *
 * @author Chuck Emmons
 */
public class TilesViewIntegrationTest {

  /**
   * Initializes the real configured plug-in and verifies every page and direct validation input.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void resolvesAllConfiguredTilesPages() throws Exception {
    Document struts = parseWebappXml("/WEB-INF/struts-config.xml");
    ModuleConfig module = new ModuleConfigImpl("");
    ControllerConfig controller = new ControllerConfig();
    Element configuredController = (Element) struts.getElementsByTagName("controller").item(0);
    controller.setProcessorClass(configuredController.getAttribute("processorClass"));
    assertTrue(
        "inputs are Tiles names or action paths",
        "false".equals(configuredController.getAttribute("inputForward")));
    module.setControllerConfig(controller);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(Globals.MODULE_KEY, module);
    ServletContext context =
        com.example.legacy.support.ServletTestSupport.createServletContext(attributes);
    ServletConfig servletConfig = org.mockito.Mockito.mock(ServletConfig.class);
    org.mockito.Mockito.when(servletConfig.getServletContext()).thenReturn(context);
    org.mockito.Mockito.when(servletConfig.getInitParameterNames())
        .thenAnswer(call -> Collections.emptyEnumeration());
    org.mockito.Mockito.when(servletConfig.getServletName()).thenReturn("action");
    ActionServlet servlet =
        new ActionServlet() {
          /**
           * Supplies the controlled application context to the real Tiles components.
           *
           * @return the servlet context
           */
          @Override
          public ServletContext getServletContext() {
            return context;
          }

          /**
           * Supplies empty servlet parameters, leaving Tiles configuration in struts-config.xml.
           *
           * @return the servlet config
           */
          @Override
          public ServletConfig getServletConfig() {
            return servletConfig;
          }
        };
    NodeList plugins = struts.getElementsByTagName("plug-in");
    TilesPlugin plugin = null;
    for (int i = 0; i < plugins.getLength(); i++) {
      Element element = (Element) plugins.item(i);
      if (TilesPlugin.class.getName().equals(element.getAttribute("className"))) {
        PlugInConfig config = new PlugInConfig();
        config.setClassName(TilesPlugin.class.getName());
        NodeList properties = element.getElementsByTagName("set-property");
        for (int j = 0; j < properties.getLength(); j++) {
          Element property = (Element) properties.item(j);
          config.addProperty(property.getAttribute("property"), property.getAttribute("value"));
        }
        assertTrue(
            "Tiles DTD validation is enabled",
            "true".equals(config.getProperties().get("definitions-parser-validate")));
        module.addPlugInConfig(config);
        plugin = new TilesPlugin();
        plugin.setCurrentPlugInConfigObject(config);
        plugin.setModuleAware(
            Boolean.parseBoolean((String) config.getProperties().get("moduleAware")));
        plugin.init(servlet, module);
      }
    }
    assertTrue(
        "Tiles processor and plug-in configured",
        plugin != null
            && TilesRequestProcessor.class.getName().equals(controller.getProcessorClass()));
    try {
      Processor processor = new Processor(servlet, module);
      for (com.example.legacy.web.PageView pageView : com.example.legacy.web.PageView.values()) {
        String page = pageView.getDefinition().substring("page.".length());
        HttpServletRequest request = createRequest(module);
        processor.renderTilesDefinition(
            WebUtil.createViewForward(pageView).getPath(), request, false);
        assertTrue(
            "shared layout for " + page,
            "/WEB-INF/views/layouts/main.jsp".equals(processor.layout));
        ComponentContext tile = ComponentContext.getContext(request);
        assertTrue(
            "page-specific body for " + page,
            ("/WEB-INF/views/" + page + ".jsp").equals(tile.getAttribute("body").toString()));
        for (String attribute : new String[] {"header", "messages", "footer", "body"}) {
          assertTrue(
              "inherited fragment exists: " + attribute,
              new File("src/main/webapp" + tile.getAttribute(attribute)).isFile());
        }
        assertTrue(
            "localized page title for " + page,
            com.example.legacy.util.MessagesUtil.formatMessage(
                        tile.getAttribute("titleKey").toString())
                    .length()
                > 0);
      }
      NodeList actions = struts.getElementsByTagName("action");
      for (int i = 0; i < actions.getLength(); i++) {
        String input = ((Element) actions.item(i)).getAttribute("input");
        if (input.startsWith("page.")) {
          HttpServletRequest request = createRequest(module);
          ActionErrors errors = new ActionErrors();
          errors.add(
              ActionMessages.GLOBAL_MESSAGE, new ActionMessage("validation.required", "Title"));
          request.setAttribute(Globals.ERROR_KEY, errors);
          processor.renderTilesDefinition(input, request, true);
          assertTrue(
              "validation input resolves through Tiles and retains original errors",
              "/WEB-INF/views/layouts/main.jsp".equals(processor.layout)
                  && request.getAttribute(Globals.ERROR_KEY) == errors);
        }
      }
      Document webConfiguration = parseWebappXml("/WEB-INF/web.xml");
      NodeList locations = webConfiguration.getElementsByTagName("location");
      for (int i = 0; i < locations.getLength(); i++) {
        File file = new File("src/main/webapp" + locations.item(i).getTextContent());
        String wrapper = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        assertTrue(
            "container errors insert the Tiles error definition",
            wrapper.contains("definition=\"page.error\""));
      }

    } finally {
      plugin.destroy();
    }
  }

  /**
   * Reads application XML without fetching remote configuration DTDs.
   *
   * @param path the XML resource path relative to the webapp directory
   * @return the parsed XML document
   * @throws Exception if test setup, execution, or cleanup fails
   */
  private static Document parseWebappXml(String path) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory.newDocumentBuilder().parse(new File("src/main/webapp" + path));
  }

  /**
   * Supplies an independent Mockito servlet request to real Tiles resolution.
   *
   * @param module the Struts module configuration
   * @return the mocked HTTP request with the configured attributes and session
   */
  private static HttpServletRequest createRequest(ModuleConfig module) {
    return com.example.legacy.support.ServletTestSupport.createRequest(
        module, Collections.emptyMap());
  }

  /**
   * Runs real Tiles resolution while capturing the final layout dispatch instead of running a JSP
   * engine.
   *
   * @author Chuck Emmons
   */
  private static final class Processor extends TilesRequestProcessor {
    private String layout;

    /**
     * Initializes the production processor with the plug-in's actual definitions factory.
     *
     * @param servlet the Struts action servlet
     * @param module the Struts module configuration
     * @throws Exception if test setup, execution, or cleanup fails
     */
    private Processor(ActionServlet servlet, ModuleConfig module) throws Exception {
      init(servlet, module);
    }

    /**
     * Captures the resolved layout without altering the Tiles component context.
     *
     * @param uri the request URI
     * @param request the current HTTP request
     * @param response the HTTP response to populate
     */
    @Override
    protected void doForward(String uri, HttpServletRequest request, HttpServletResponse response) {
      layout = uri;
    }

    /**
     * Resolves either a normal action result or a validation input through production Tiles logic.
     *
     * @param name the Tiles definition to render
     * @param request the current HTTP request
     * @param input whether the forward is an input forward
     * @throws Exception if test setup, execution, or cleanup fails
     */
    private void renderTilesDefinition(String name, HttpServletRequest request, boolean input)
        throws Exception {
      layout = null;
      if (input) {
        internalModuleRelativeForward(name, request, null);
      } else {
        processForwardConfig(request, null, new ActionForward(name));
      }
    }
  }
}
