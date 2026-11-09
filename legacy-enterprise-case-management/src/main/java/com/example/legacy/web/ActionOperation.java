package com.example.legacy.web;

/**
 * Server-configured Struts operation parameters shared by Actions and validation adapters.
 *
 * @author Chuck Emmons
 */
public enum ActionOperation {
  /** The login action operation. */
  LOGIN("login"),
  /** The authenticate action operation. */
  AUTHENTICATE("authenticate"),
  /** The logout action operation. */
  LOGOUT("logout"),
  /** The list action operation. */
  LIST("list"),
  /** The new action operation. */
  NEW("new"),
  /** The view action operation. */
  VIEW("view"),
  /** The create action operation. */
  CREATE("create"),
  /** The update action operation. */
  UPDATE("update"),
  /** The assign action operation. */
  ASSIGN("assign"),
  /** The transition action operation. */
  TRANSITION("transition"),
  /** The comment action operation. */
  COMMENT("comment"),
  /** The archive action operation. */
  ARCHIVE("archive"),
  /** The upload action operation. */
  UPLOAD("upload"),
  /** The download action operation. */
  DOWNLOAD("download"),
  /** The role action operation. */
  ROLE("role"),
  /** The disable action operation. */
  DISABLE("disable"),
  /** The page action operation. */
  PAGE("page"),
  /** The csv action operation. */
  CSV("csv"),
  /** The read action operation. */
  READ("read"),
  /** The case action operation. */
  CASE("case"),
  /** The cases action operation. */
  CASES("cases"),
  /** The users action operation. */
  USERS("users"),
  /** The audit action operation. */
  AUDIT("audit"),
  /** The notifications action operation. */
  NOTIFICATIONS("notifications"),
  /** The reports action operation. */
  REPORTS("reports"),
  /** The dashboard action operation. */
  DASHBOARD("dashboard");
  private final String parameter;

  /**
   * Captures the existing struts-config.xml parameter value.
   *
   * @param parameter the operation parameter from the Struts mapping
   */
  ActionOperation(String parameter) {
    this.parameter = parameter;
  }

  /**
   * Compares a configured parameter without accepting arbitrary enum conversion failures.
   *
   * @param value the external code to compare
   * @return {@code true} if the supplied code matches this value; {@code false} otherwise
   */
  public boolean matchesParameter(String value) {
    return parameter.equals(value);
  }

  /**
   * Returns the external parameter used by Struts configuration and validator rule keys.
   *
   * @return the external parameter used by Struts configuration and validator rule keys
   */
  public String getParameter() {
    return parameter;
  }
}
