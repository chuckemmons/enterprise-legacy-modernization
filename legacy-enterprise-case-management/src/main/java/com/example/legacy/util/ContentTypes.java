package com.example.legacy.util;

/**
 * Central names for ContentTypes; values preserve the application's external contracts.
 *
 * @author Chuck Emmons
 */
public final class ContentTypes {
  /** Media type for binary: {@code application/octet-stream}. */
  public static final String BINARY = "application/octet-stream";

  /** Media type for pdf: {@code application/pdf}. */
  public static final String PDF = "application/pdf";

  /** Media type for jpeg: {@code image/jpeg}. */
  public static final String JPEG = "image/jpeg";

  /** Media type for png: {@code image/png}. */
  public static final String PNG = "image/png";

  /** Media type for text: {@code text/plain}. */
  public static final String TEXT = "text/plain";

  /** Media type for multipart prefix: {@code multipart/}. */
  public static final String MULTIPART_PREFIX = "multipart/";

  /** Media type for csv utf8: {@code text/csv; charset=UTF-8}. */
  public static final String CSV_UTF8 = "text/csv; charset=UTF-8";

  /** Prevents instantiation of this constants class. */
  private ContentTypes() {}

  /**
   * Checks the same attachment allowlist in form display validation and business validation.
   *
   * @param type the attachment's MIME content type
   * @return {@code true} if the MIME type is in the attachment allowlist; {@code false} otherwise
   */
  public static boolean isSupportedAttachment(String type) {
    return java.util.Arrays.asList(PDF, JPEG, PNG, TEXT, BINARY).contains(type);
  }
}
