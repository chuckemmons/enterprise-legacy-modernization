package com.example.legacy.web;

import com.example.legacy.util.ValidationLimits;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.servlet.ServletException;
import org.apache.commons.validator.Arg;
import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.Var;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.validator.ValidatorPlugIn;

/**
 * Resolves symbolic XML validation limits from the same constants used by services and JSP
 * controls.
 *
 * @author Chuck Emmons
 */
public final class ApplicationValidatorPlugIn extends ValidatorPlugIn {
  private static final String LIMIT_PREFIX = "limit.";
  private ModuleConfig module;

  /**
   * Captures module rule names before the framework initializes and publishes validator resources.
   *
   * @param servlet the Struts action servlet
   * @param module the Struts module configuration
   * @throws ServletException if servlet initialization or request processing fails
   */
  @Override
  public void init(ActionServlet servlet, ModuleConfig module) throws ServletException {
    this.module = module;
    super.init(servlet, module);
  }

  /**
   * Loads normal Struts rules, then binds shared limits before any request can validate a form.
   *
   * @throws IOException if an I/O operation fails
   * @throws ServletException if servlet initialization or request processing fails
   */
  @Override
  protected void initResources() throws IOException, ServletException {
    super.initResources();
    Set<String> processed = new HashSet<>();
    for (ActionConfig action : module.findActionConfigs()) {
      FormBeanConfig bean = module.findFormBeanConfig(action.getName());
      if (bean == null) {
        continue;
      }
      String type = bean.getType();
      String key =
          type.substring(type.lastIndexOf('.') + 1)
              + (action.getParameter() == null ? "" : "." + action.getParameter());
      if (!processed.add(key)) {
        continue;
      }
      resolveFormLimits(resources.getForm(Locale.ENGLISH, key));
    }
  }

  /**
   * Resolves symbolic limits for every field in an optional operation-specific form.
   *
   * @param form the configured form, or {@code null} when the action has no validation rules
   * @throws ServletException if a symbolic limit has no shared constant
   */
  private void resolveFormLimits(Form form) throws ServletException {
    if (form == null) {
      return;
    }
    for (Object candidate : form.getFields()) {
      Field field = (Field) candidate;
      for (Object configured : field.getVars().values()) {
        resolveValidationLimit(field, (Var) configured);
      }
    }
  }

  /**
   * Replaces one symbolic variable and its optional message argument with the shared numeric limit.
   *
   * @param field the field owning the validation variable
   * @param variable the validation variable to resolve
   * @throws ServletException if the variable names an unknown shared limit
   * @throws NullPointerException if {@code variable} is {@code null}, or {@code field} is {@code
   *     null} when a symbolic limit is resolved
   */
  private void resolveValidationLimit(Field field, Var variable) throws ServletException {
    String symbolic = variable.getValue();
    if (symbolic == null || !symbolic.startsWith(LIMIT_PREFIX)) {
      return;
    }

    String resolved = ValidationLimits.getValues().get(symbolic.substring(LIMIT_PREFIX.length()));
    if (resolved == null) {
      throw new ServletException("Unknown validation limit: " + symbolic);
    }

    variable.setValue(resolved);

    Arg argument = field.getArg(variable.getName(), 1);
    if (argument != null) {
      argument.setKey(resolved);
    }
  }
}
