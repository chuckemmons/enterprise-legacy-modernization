package com.example.legacy.util;

/**
 * Shared business/presentation limits; database metadata retains the corresponding SQL constraints.
 *
 * @author Chuck Emmons
 */
public final class ValidationLimits {
  /** Validation bound for search. */
  public static final int SEARCH = 80;

  /** Validation bound for title. */
  public static final int TITLE = 180;

  /** Validation bound for description. */
  public static final int DESCRIPTION = 4000;

  /** Validation bound for category. */
  public static final int CATEGORY = 60;

  /** Validation bound for department. */
  public static final int DEPARTMENT = 80;

  /** Validation bound for username. */
  public static final int USERNAME = 60;

  /** Validation bound for display name. */
  public static final int DISPLAY_NAME = 120;

  /** Validation bound for email. */
  public static final int EMAIL = 254;

  /** Validation bound for details. */
  public static final int DETAILS = 4000;

  /** Validation bound for subject. */
  public static final int SUBJECT = 180;

  /** Validation bound for comment. */
  public static final int COMMENT = 4000;

  /** Validation bound for password min. */
  public static final int PASSWORD_MIN = 12;

  /** Validation bound for attachment bytes. */
  public static final int ATTACHMENT_BYTES = 2 * 1024 * 1024;

  /** Validation bound for case page size. */
  public static final int CASE_PAGE_SIZE = 25;

  /** Validation bound for audit page size. */
  public static final int AUDIT_PAGE_SIZE = 50;

  /** Validation bound for export rows. */
  public static final int EXPORT_ROWS = 5000;

  /** Validation bound for max page. */
  public static final int MAX_PAGE = 100000;

  /** Validation bound for username pattern. */
  public static final String USERNAME_PATTERN = "[a-z][a-z0-9._-]{2,59}";

  /** Prevents instantiation of this limits class. */
  private ValidationLimits() {}

  /**
   * Exposes named limits to validator configuration and JSP EL without reflection or writable
   * shared state.
   *
   * @return the values indexed by their keys
   */
  public static java.util.Map<String, String> getValues() {
    java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
    values.put("SEARCH", Integer.toString(SEARCH));
    values.put("TITLE", Integer.toString(TITLE));
    values.put("DESCRIPTION", Integer.toString(DESCRIPTION));
    values.put("CATEGORY", Integer.toString(CATEGORY));
    values.put("DEPARTMENT", Integer.toString(DEPARTMENT));
    values.put("USERNAME", Integer.toString(USERNAME));
    values.put("DISPLAY_NAME", Integer.toString(DISPLAY_NAME));
    values.put("EMAIL", Integer.toString(EMAIL));
    values.put("DETAILS", Integer.toString(DETAILS));
    values.put("SUBJECT", Integer.toString(SUBJECT));
    values.put("COMMENT", Integer.toString(COMMENT));
    values.put("PASSWORD_MIN", Integer.toString(PASSWORD_MIN));
    values.put("ATTACHMENT_BYTES", Integer.toString(ATTACHMENT_BYTES));
    values.put("CASE_PAGE_SIZE", Integer.toString(CASE_PAGE_SIZE));
    values.put("AUDIT_PAGE_SIZE", Integer.toString(AUDIT_PAGE_SIZE));
    values.put("EXPORT_ROWS", Integer.toString(EXPORT_ROWS));
    values.put("MAX_PAGE", Integer.toString(MAX_PAGE));
    values.put("USERNAME_INPUT_PATTERN", "(?i)^" + USERNAME_PATTERN + "$");
    values.put("INPUT_DATE_PATTERN", DatesUtil.INPUT_PATTERN);
    return java.util.Collections.unmodifiableMap(values);
  }
}
