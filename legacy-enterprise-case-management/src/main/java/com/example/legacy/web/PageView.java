package com.example.legacy.web;

/**
 * Logical Tiles page definitions returned by presentation adapters.
 *
 * @author Chuck Emmons
 */
public enum PageView {
  /** The login page view. */
  LOGIN("page.login"),
  /** The dashboard page view. */
  DASHBOARD("page.dashboard"),
  /** The cases page view. */
  CASES("page.cases"),
  /** The case new page view. */
  CASE_NEW("page.case-new"),
  /** The case view page view. */
  CASE_VIEW("page.case-view"),
  /** The users page view. */
  USERS("page.users"),
  /** The audit page view. */
  AUDIT("page.audit"),
  /** The reports page view. */
  REPORTS("page.reports"),
  /** The notifications page view. */
  NOTIFICATIONS("page.notifications"),
  /** The upload errors page view. */
  UPLOAD_ERRORS("page.upload-errors"),
  /** The error page view. */
  ERROR("page.error");
  private final String definition;

  /**
   * Captures the exact Tiles definition name declared in tiles-defs.xml.
   *
   * @param definition the Tiles definition name
   */
  PageView(String definition) {
    this.definition = definition;
  }

  /**
   * Returns the logical definition resolved by TilesRequestProcessor.
   *
   * @return the logical definition resolved by TilesRequestProcessor
   */
  public String getDefinition() {
    return definition;
  }
}
