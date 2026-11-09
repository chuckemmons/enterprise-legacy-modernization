package com.example.legacy.web;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.util.MessageKeys;

/**
 * Parses optional form fields without reading directly from servlet requests.
 *
 * @author Chuck Emmons
 */
public final class FormDataUtil {
  /** Prevents construction of this parsing utility. */
  private FormDataUtil() {}

  /**
   * Parses an optional positive identifier; absent identifiers remain zero until the chosen service
   * requires one.
   *
   * @param value the submitted record identifier
   * @return the positive identifier, or zero when no identifier was submitted
   * @throws com.example.legacy.service.exception.BusinessValidationException if the submitted
   *     identifier is malformed or not positive
   */
  public static long parseRecordId(String value) {
    if (value == null || value.trim().isEmpty()) {
      return 0;
    }
    try {
      long id = Long.parseLong(value);
      if (id > 0) {
        return id;
      }
    } catch (NumberFormatException ignored) {
      // Malformed input is reported through the same business error as nonpositive IDs.
    }
    throw new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID);
  }

  /**
   * Parses a bounded zero-based page index, defaulting malformed values to the first page.
   *
   * @param value the submitted page index
   * @return the bounded zero-based page index, defaulting to zero for malformed input
   */
  public static int parsePageIndex(String value) {
    try {
      return Math.min(
          com.example.legacy.util.ValidationLimits.MAX_PAGE, Math.max(0, Integer.parseInt(value)));
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  /**
   * Parses an optional optimistic-lock version, rejecting malformed submitted values.
   *
   * @param value the submitted optimistic-lock version
   * @return the submitted version, or {@code null} if no version was supplied
   * @throws BusinessValidationException if the submitted version is not a valid integer
   */
  public static Integer parseRecordVersion(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException exception) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_VERSION);
    }
  }
}
