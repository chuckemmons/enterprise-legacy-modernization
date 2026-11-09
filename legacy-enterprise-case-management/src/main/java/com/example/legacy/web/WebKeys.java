package com.example.legacy.web;

/**
 * Central names for WebKeys; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class WebKeys {
  /** Request or session attribute for request id. */
  public static final String REQUEST_ID = "requestId";

  /** Request or session attribute for record id. */
  public static final String RECORD_ID = "recordId";

  /** Request or session attribute for error logged. */
  public static final String ERROR_LOGGED = "errorLogged";

  /** Request or session attribute for actor. */
  public static final String ACTOR = "actor";

  /** Request or session attribute for role. */
  public static final String ROLE = "role";

  /** Request or session attribute for display name. */
  public static final String DISPLAY_NAME = "displayName";

  /** Request or session attribute for csrf. */
  public static final String CSRF = "csrf";

  /** Request or session attribute for validation operation. */
  public static final String VALIDATION_OPERATION = "validationOperation";

  /** Request or session attribute for error. */
  public static final String ERROR = "error";

  /** Request or session attribute for message. */
  public static final String MESSAGE = "message";

  /** Request or session attribute for page number. */
  public static final String PAGE_NUMBER = "pageNumber";

  /** Request or session attribute for legacy page. */
  public static final String LEGACY_PAGE = "page";

  /** Prevents instantiation of this constants class. */
  private WebKeys() {}
}
