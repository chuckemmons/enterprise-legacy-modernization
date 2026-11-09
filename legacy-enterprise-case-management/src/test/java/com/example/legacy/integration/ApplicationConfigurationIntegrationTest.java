package com.example.legacy.integration;

import static org.junit.Assert.assertTrue;

import com.example.legacy.support.ValidatorTestSupport;
import com.example.legacy.util.ValidationLimits;
import com.example.legacy.web.ViewOptionsUtil;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.validator.Field;
import org.apache.struts.action.ActionForm;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Verifies the actual Struts, validator, JSP-limit, and DataSource configuration contracts.
 *
 * @author Chuck Emmons
 */
public class ApplicationConfigurationIntegrationTest {
  /**
   * Verifies real validator resources and JSP controls use the same service limit definitions.
   *
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void sharesValidationLimitsWithViews() {
    Field title =
        ValidatorTestSupport.getValidatorResources()
            .getForm(Locale.ENGLISH, "CaseForm.create")
            .getField("title");
    assertTrue(
        "XML symbolic title limit resolves from Java",
        Integer.toString(ValidationLimits.TITLE).equals(title.getVarValue("maxlength")));
    assertTrue(
        "validation message arguments receive the resolved limit",
        Integer.toString(ValidationLimits.TITLE).equals(title.getArg("maxlength", 1).getKey()));
    assertTrue(
        "JSP limits use the same definitions as validation and services",
        ValidationLimits.getValues().equals(new ViewOptionsUtil().getLimits()));
  }

  /**
   * Checks JNDI is the deployment default and standalone configuration explicitly selects the owned
   * pooled provider.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void usesJndiByDefaultAndStandalonePoolWhenConfigured() throws Exception {
    org.hibernate.cfg.Configuration configuration =
        new org.hibernate.cfg.Configuration().configure();
    String jndi = configuration.getProperty("hibernate.connection.datasource");
    assertTrue(
        "web configuration uses the container-managed DataSource",
        "java:comp/env/jdbc/LegacyCases".equals(jndi));
    assertTrue(
        "web mappings contain no JDBC credentials or built-in pool configuration",
        configuration.getProperty("hibernate.connection.url") == null
            && configuration.getProperty("hibernate.connection.password") == null
            && configuration.getProperty("hibernate.connection.pool_size") == null);
    javax.xml.parsers.DocumentBuilderFactory parser =
        javax.xml.parsers.DocumentBuilderFactory.newInstance();
    org.w3c.dom.Document template =
        parser
            .newDocumentBuilder()
            .parse(new java.io.File("config/tomcat/legacy-cases.xml.example"));
    String resource =
        ((org.w3c.dom.Element) template.getElementsByTagName("Resource").item(0))
            .getAttribute("name");
    assertTrue(
        "external Context template declares the expected JNDI resource",
        jndi.endsWith("/" + resource));
    com.example.legacy.db.DatabaseUtil.configureStandaloneConnectionPool(
        configuration, "jdbc:postgresql://localhost/unused", "test", "");
    assertTrue(
        "explicit standalone configuration uses the bounded provider without resolving JNDI",
        configuration.getProperty("hibernate.connection.datasource") == null
            && com.example.legacy.db.LocalConnectionProvider.class
                .getName()
                .equals(configuration.getProperty("hibernate.connection.provider_class")));
  }

  /**
   * Checks every configured route has an instantiable request-scoped form and matching
   * getData/setData contract.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if an asserted result or expected interaction does not match the
   *     observed behavior
   */
  @Test
  public void bindsConfiguredRoutesToRequestScopedForms() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    Document configuration =
        factory.newDocumentBuilder().parse(new File("src/main/webapp/WEB-INF/struts-config.xml"));
    Map<String, Class<?>> forms = new HashMap<>();
    NodeList beans = configuration.getElementsByTagName("form-bean");
    for (int i = 0; i < beans.getLength(); i++) {
      Element bean = (Element) beans.item(i);
      Class<?> type = Class.forName(bean.getAttribute("type"));
      assertTrue("configured Struts form type", ActionForm.class.isAssignableFrom(type));
      Object form = type.getDeclaredConstructor().newInstance();
      Method getData = type.getMethod("getData");
      Object dto = getData.invoke(form);
      type.getMethod("setData", getData.getReturnType()).invoke(form, dto);
      forms.put(bean.getAttribute("name"), type);
    }
    NodeList actions = configuration.getElementsByTagName("action");
    assertTrue("all application routes checked", actions.getLength() > 20);
    for (int i = 0; i < actions.getLength(); i++) {
      Element action = (Element) actions.item(i);
      assertTrue(
          "form bound on " + action.getAttribute("path"),
          forms.containsKey(action.getAttribute("name"))
              && "request".equals(action.getAttribute("scope")));
    }
  }
}
