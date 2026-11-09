package com.example.legacy.web;

/**
 * Central names for HttpConstants; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class HttpConstants {
  /** HTTP contract value for request id header. */
  public static final String REQUEST_ID_HEADER = "X-Request-ID";

  /** HTTP contract value for case export disposition. */
  public static final String CASE_EXPORT_DISPOSITION = "attachment; filename=\"cases.csv\"";

  /** HTTP contract value for content disposition. */
  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  /** HTTP contract value for content type options. */
  public static final String CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

  /** HTTP contract value for no sniff. */
  public static final String NO_SNIFF = "nosniff";

  /** HTTP contract value for frame options. */
  public static final String FRAME_OPTIONS = "X-Frame-Options";

  /** HTTP contract value for deny. */
  public static final String DENY = "DENY";

  /** HTTP contract value for cache control. */
  public static final String CACHE_CONTROL = "Cache-Control";

  /** HTTP contract value for no store. */
  public static final String NO_STORE = "no-store";

  /** HTTP contract value for post. */
  public static final String POST = "POST";

  /** Prevents instantiation of this constants class. */
  private HttpConstants() {}
}
