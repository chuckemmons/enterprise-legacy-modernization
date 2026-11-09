package com.example.legacy.web;

/**
 * Bound form property names used by Java validation; XML/JSP bindings retain these public
 * contracts.
 *
 * @author Chuck Emmons
 */
public final class FormFields {
  /** Bound form property for id. */
  public static final String ID = "id";

  /** Bound form property for version. */
  public static final String VERSION = "version";

  /** Bound form property for file. */
  public static final String FILE = "file";

  /** Bound form property for priority. */
  public static final String PRIORITY = "priority";

  /** Bound form property for destination. */
  public static final String DESTINATION = "destination";

  /** Bound form property for status. */
  public static final String STATUS = "status";

  /** Bound form property for role. */
  public static final String ROLE = "role";

  /** Bound form property for page. */
  public static final String PAGE = "page";

  /** Prevents instantiation of this constants class. */
  private FormFields() {}
}
