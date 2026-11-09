package com.example.legacy.support;

import com.example.legacy.web.BasePageForm;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.validator.ValidatorResources;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.util.MessageResources;
import org.apache.struts.validator.ValidatorPlugIn;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Boots the configured production ValidatorPlugIn for form tests without starting a servlet
 * container.
 *
 * @author Chuck Emmons
 */
public final class ValidatorTestSupport {
  private static final Map<String, Object> attributes = new HashMap<>();
  private static final ModuleConfig module = new ModuleConfigImpl("");
  private static final ServletContext context =
      com.example.legacy.support.ServletTestSupport.createServletContext(attributes);
  private static final ActionServlet servlet =
      new ActionServlet() {
        /**
         * Supplies the test context to the real ValidatorForm/plug-in implementation.
         *
         * @return the servlet context
         */
        @Override
        public ServletContext getServletContext() {
          return context;
        }
      };

  static {
    try {
      attributes.put(Globals.MODULE_KEY, module);
      attributes.put(
          Globals.MESSAGES_KEY, MessageResources.getMessageResources("ApplicationResources"));
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      Document config =
          factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/struts-config.xml"));
      NodeList beans = config.getElementsByTagName("form-bean");
      for (int i = 0; i < beans.getLength(); i++) {
        Element element = (Element) beans.item(i);
        org.apache.struts.config.FormBeanConfig bean =
            new org.apache.struts.config.FormBeanConfig();
        bean.setName(element.getAttribute("name"));
        bean.setType(element.getAttribute("type"));
        module.addFormBeanConfig(bean);
      }
      NodeList actions = config.getElementsByTagName("action");
      for (int i = 0; i < actions.getLength(); i++) {
        Element element = (Element) actions.item(i);
        org.apache.struts.config.ActionConfig action = new org.apache.struts.config.ActionConfig();
        action.setPath(element.getAttribute("path"));
        action.setName(element.getAttribute("name"));
        if (element.hasAttribute("parameter")) {
          action.setParameter(element.getAttribute("parameter"));
        }
        module.addActionConfig(action);
      }
      NodeList plugins = config.getElementsByTagName("plug-in");
      boolean found = false;
      for (int i = 0; i < plugins.getLength(); i++) {
        Element element = (Element) plugins.item(i);
        if (com.example.legacy.web.ApplicationValidatorPlugIn.class
            .getName()
            .equals(element.getAttribute("className"))) {
          ValidatorPlugIn plugin = new com.example.legacy.web.ApplicationValidatorPlugIn();
          NodeList properties = element.getElementsByTagName("set-property");
          for (int j = 0; j < properties.getLength(); j++) {
            Element property = (Element) properties.item(j);
            if ("pathnames".equals(property.getAttribute("property"))) {
              plugin.setPathnames(property.getAttribute("value"));
            }
          }
          plugin.init(servlet, module);
          found = true;
        }
      }
      if (!found) {
        throw new AssertionError("ValidatorPlugIn must be configured");
      }
    } catch (Exception failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  /** Prevents construction of this test fixture utility. */
  private ValidatorTestSupport() {}

  /**
   * Attaches the initialized Struts servlet as RequestProcessor would before validating a form.
   *
   * @param <T> the concrete form type
   * @param form the form containing the submitted values
   * @return the supplied form attached to the initialized Struts servlet
   * @throws NullPointerException if {@code form} is {@code null}
   */
  public static <T extends BasePageForm> T prepareForm(T form) {
    form.setServlet(servlet);
    return form;
  }

  /**
   * Returns the default module used to locate validator and message resources.
   *
   * @return the default module used to locate validator and message resources
   */
  public static ModuleConfig getModuleConfiguration() {
    return module;
  }

  /**
   * Returns production XML rules loaded by the actual plug-in, not a replacement validation
   * implementation.
   *
   * @return production XML rules loaded by the actual plug-in, not a replacement validation
   *     implementation
   */
  public static ValidatorResources getValidatorResources() {
    return (ValidatorResources) attributes.get(ValidatorPlugIn.VALIDATOR_KEY);
  }
}
