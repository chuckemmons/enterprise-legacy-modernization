package com.example.legacy.unit.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.legacy.util.ValidationLimits;
import com.example.legacy.web.ApplicationValidatorPlugIn;
import com.example.legacy.web.CaseForm;
import java.io.File;
import java.util.Locale;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.commons.validator.Arg;
import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.Var;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.junit.Test;
import org.mockito.MockedConstruction;

/**
 * Exercises symbolic-limit binding through the plug-in's public initialization lifecycle.
 *
 * @author Chuck Emmons
 */
public class ApplicationValidatorPlugInTest {

  /**
   * Resolves shared limits once and keeps literal variables unchanged.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void resolvesSharedLimitsOnceAndKeepsLiteralVariablesUnchanged() throws Exception {
    Field field = new Field();
    field.setProperty("title");
    Var limit = variable("maxlength", "limit.TITLE");
    Var literal = variable("literal", "42");
    Var absent = variable("absent", null);
    Var noArgument = variable("other", "limit.CATEGORY");
    field.addVar(limit);
    field.addVar(literal);
    field.addVar(absent);
    field.addVar(noArgument);
    Arg argument = new Arg();
    argument.setName("maxlength");
    argument.setPosition(1);
    argument.setKey("limit.TITLE");
    field.addArg(argument);
    Form form = new Form();
    form.setName("CaseForm.create");
    form.addField(field);
    ModuleConfigImpl module = module();
    ActionServlet servlet = servlet();
    try (MockedConstruction<ValidatorResources> resources =
        mockConstruction(
            ValidatorResources.class,
            (value, context) ->
                when(value.getForm(Locale.ENGLISH, "CaseForm.create")).thenReturn(form)); ) {
      ApplicationValidatorPlugIn plugin = new ApplicationValidatorPlugIn();
      plugin.setPathnames("/unit-validation.xml");
      plugin.init(servlet, module);
      assertEquals(Integer.toString(ValidationLimits.TITLE), limit.getValue());
      assertEquals(Integer.toString(ValidationLimits.TITLE), argument.getKey());
      assertEquals("42", literal.getValue());
      assertNull(absent.getValue());
      assertEquals(Integer.toString(ValidationLimits.CATEGORY), noArgument.getValue());
      verify(resources.constructed().get(0), times(1)).getForm(Locale.ENGLISH, "CaseForm.create");
    }
  }

  /**
   * Fails initialization for unknown shared limits.
   *
   * @throws Exception if test setup, execution, or cleanup fails
   * @throws AssertionError if the observed behavior violates the tested contract
   */
  @Test
  public void failsInitializationForUnknownSharedLimits() throws Exception {
    Field field = new Field();
    field.setProperty("title");
    field.addVar(variable("maxlength", "limit.UNKNOWN"));
    Form form = new Form();
    form.addField(field);
    ActionServlet servlet = servlet();
    ModuleConfigImpl module = module();
    try (MockedConstruction<ValidatorResources> resources =
        mockConstruction(
            ValidatorResources.class,
            (value, context) ->
                when(value.getForm(Locale.ENGLISH, "CaseForm.create")).thenReturn(form)); ) {
      ApplicationValidatorPlugIn plugin = new ApplicationValidatorPlugIn();
      plugin.setPathnames("/unit-validation.xml");
      ServletException failure =
          assertThrows(ServletException.class, () -> plugin.init(servlet, module));
      assertTrue(failure instanceof javax.servlet.UnavailableException);
      assertTrue(failure.getMessage().contains("unit-validation.xml"));
    }
  }

  /**
   * Variable.
   *
   * @param name the configured name
   * @param value the supplied value
   * @return the value prepared for the test
   */
  private Var variable(String name, String value) {
    Var result = new Var();
    result.setName(name);
    result.setValue(value);
    return result;
  }

  /**
   * Module.
   *
   * @return the value prepared for the test
   */
  private ModuleConfigImpl module() {
    ModuleConfigImpl module = new ModuleConfigImpl("");
    FormBeanConfig bean = new FormBeanConfig();
    bean.setName("caseForm");
    bean.setType(CaseForm.class.getName());
    module.addFormBeanConfig(bean);
    addAction(module, "/first", "caseForm", "create");
    addAction(module, "/duplicate", "caseForm", "create");
    addAction(module, "/without-rules", "caseForm", null);
    addAction(module, "/without-bean", "missing", "create");
    return module;
  }

  /**
   * Add action.
   *
   * @param module the module receiving the action mapping
   * @param path the action path
   * @param name the configured name
   * @param operation the mapped operation
   */
  private void addAction(ModuleConfigImpl module, String path, String name, String operation) {
    ActionConfig action = new ActionConfig();
    action.setPath(path);
    action.setName(name);
    action.setParameter(operation);
    module.addActionConfig(action);
  }

  /**
   * Servlet.
   *
   * @return the value prepared for the test
   * @throws Exception if test setup, execution, or cleanup fails
   */
  private ActionServlet servlet() throws Exception {
    ServletContext context = mock(ServletContext.class);
    when(context.getResource("/unit-validation.xml"))
        .thenReturn(new File("pom.xml").toURI().toURL());
    ActionServlet servlet = mock(ActionServlet.class);
    when(servlet.getServletContext()).thenReturn(context);
    return servlet;
  }
}
