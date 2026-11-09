package com.example.legacy.web;

import com.example.legacy.domain.CaseStatus;
import com.example.legacy.domain.Priority;
import com.example.legacy.domain.Role;
import com.example.legacy.util.CaseDefaults;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless JSP adapter exposing shared enum choices and defaults to separately translated Tiles
 * components.
 *
 * @author Chuck Emmons
 */
public final class ViewOptionsUtil {
  /**
   * Returns an independent set of role option codes.
   *
   * @return an independent set of role option codes
   */
  public String[] getRoles() {
    return Role.getCodes();
  }

  /**
   * Returns an independent set of workflow status option codes.
   *
   * @return an independent set of workflow status option codes
   */
  public String[] getStatuses() {
    return CaseStatus.getCodes();
  }

  /**
   * Returns an independent set of case priority option codes.
   *
   * @return an independent set of case priority option codes
   */
  public String[] getPriorities() {
    return Priority.getCodes();
  }

  /**
   * Returns named role codes for JSP permission comparisons.
   *
   * @return named role codes for JSP permission comparisons
   */
  public Map<String, String> getRoleCodes() {
    Map<String, String> codes = new LinkedHashMap<>();
    for (Role role : Role.values()) {
      codes.put(role.name(), role.name());
    }
    return Collections.unmodifiableMap(codes);
  }

  /**
   * Returns the default newly opened case priority.
   *
   * @return the default newly opened case priority
   */
  public String getDefaultPriority() {
    return Priority.MEDIUM.name();
  }

  /**
   * Returns the default role for account creation controls.
   *
   * @return the default role for account creation controls
   */
  public String getDefaultRole() {
    return Role.WORKER.name();
  }

  /**
   * Returns the suggested case category without restricting service-side category input.
   *
   * @return the suggested case category without restricting service-side category input
   */
  public String getDefaultCategory() {
    return CaseDefaults.CATEGORY;
  }

  /**
   * Returns the suggested department for case/account creation.
   *
   * @return the suggested department for case/account creation
   */
  public String getDefaultDepartment() {
    return CaseDefaults.DEPARTMENT;
  }

  /**
   * Exposes shared navigation route constants to JSP EL.
   *
   * @return the values indexed by their keys
   */
  public Map<String, String> getRoutes() {
    return Routes.getPaths();
  }

  /**
   * Exposes shared HTTP/session keys to JSP EL without duplicating their values in pages.
   *
   * @return the values indexed by their keys
   */
  public Map<String, String> getKeys() {
    Map<String, String> keys = new LinkedHashMap<>();
    keys.put("ACTOR", WebKeys.ACTOR);
    keys.put("ROLE", WebKeys.ROLE);
    keys.put("DISPLAY_NAME", WebKeys.DISPLAY_NAME);
    keys.put("CSRF", WebKeys.CSRF);
    keys.put("VALIDATION_OPERATION", WebKeys.VALIDATION_OPERATION);
    keys.put("ERROR", WebKeys.ERROR);
    keys.put("MESSAGE", WebKeys.MESSAGE);
    keys.put("PAGE_NUMBER", WebKeys.PAGE_NUMBER);
    keys.put("LEGACY_PAGE", WebKeys.LEGACY_PAGE);
    return Collections.unmodifiableMap(keys);
  }

  /**
   * Returns the shared business limits used by input controls and Commons Validator.
   *
   * @return the shared business limits used by input controls and Commons Validator
   */
  public Map<String, String> getLimits() {
    return com.example.legacy.util.ValidationLimits.getValues();
  }
}
