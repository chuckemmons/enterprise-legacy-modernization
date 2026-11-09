package com.example.legacy.util;

/**
 * Central names for EnvironmentKeys; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class EnvironmentKeys {
  /** Environment variable for db url. */
  public static final String DB_URL = "DB_URL";

  /** Environment variable for db user. */
  public static final String DB_USER = "DB_USER";

  /** Environment variable for db password. */
  public static final String DB_PASSWORD = "DB_PASSWORD";

  /** Environment variable for case admin user. */
  public static final String CASE_ADMIN_USER = "CASE_ADMIN_USER";

  /** Environment variable for case admin password. */
  public static final String CASE_ADMIN_PASSWORD = "CASE_ADMIN_PASSWORD";

  /** Environment variable for case demo password. */
  public static final String CASE_DEMO_PASSWORD = "CASE_DEMO_PASSWORD";

  /** Environment variable for smtp host. */
  public static final String SMTP_HOST = "SMTP_HOST";

  /** Environment variable for smtp from. */
  public static final String SMTP_FROM = "SMTP_FROM";

  /** Environment variable for smtp port. */
  public static final String SMTP_PORT = "SMTP_PORT";

  /** Environment variable for smtp user. */
  public static final String SMTP_USER = "SMTP_USER";

  /** Environment variable for smtp password. */
  public static final String SMTP_PASSWORD = "SMTP_PASSWORD";

  /** Prevents instantiation of this constants class. */
  private EnvironmentKeys() {}
}
