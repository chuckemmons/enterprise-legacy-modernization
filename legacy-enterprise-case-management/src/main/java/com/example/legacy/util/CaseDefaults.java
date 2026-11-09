package com.example.legacy.util;

/**
 * Central names for CaseDefaults; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class CaseDefaults {
  /** Initial case value for category. */
  public static final String CATEGORY = "GENERAL";

  /** Initial case value for department. */
  public static final String DEPARTMENT = "Operations";

  /** Initial case value for number format. */
  public static final String NUMBER_FORMAT = "CASE-%06d";

  /** Initial case value for temporary number prefix. */
  public static final String TEMPORARY_NUMBER_PREFIX = "TMP-";

  /** Prevents instantiation of this constants class. */
  private CaseDefaults() {}
}
