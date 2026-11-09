package com.example.legacy.util;

import com.example.legacy.service.exception.BusinessValidationException;
import com.example.legacy.service.exception.MessageArgument;

/**
 * Framework-independent application argument validation with message keys instead of formatted UI
 * text.
 *
 * @author Chuck Emmons
 */
public final class ServiceValidationUtil {

  /** Prevents instantiation of the shared validation helper. */
  private ServiceValidationUtil() {}

  /**
   * Checks the minimum strength required when setting a new account password.
   *
   * @param password the password to validate without trimming or changing it
   * @return the original password
   * @throws BusinessValidationException if the password is missing or too short
   */
  public static String requirePassword(String password) {
    if (password == null || password.length() < ValidationLimits.PASSWORD_MIN) {
      throw new BusinessValidationException(
          MessageKeys.BUSINESS_PASSWORD_MUST_BE_AT_LEAST_12_CHARACTERS);
    }
    return password;
  }

  /**
   * Requires a positive persistent identity before attempting a record lookup.
   *
   * @param id the persistent record identifier
   * @return the validated positive record identifier
   * @throws BusinessValidationException if {@code id} is not positive
   */
  public static long requireRecordId(long id) {
    if (id < 1) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_INVALID_RECORD_ID);
    }
    return id;
  }

  /**
   * Validates required input while retaining a field-label key for later localization.
   *
   * @param value the required input text
   * @param maximum the maximum permitted text length
   * @param labelKey the message resource key for the field label
   * @return the normalized, nonempty field value
   * @throws BusinessValidationException if the normalized value is empty or exceeds the maximum
   *     length
   * @throws NullPointerException if {@code labelKey} is {@code null}
   */
  public static String requireText(String value, int maximum, String labelKey) {
    String result = StringsUtil.normalizeText(value, maximum);
    if (result.isEmpty()) {
      throw new BusinessValidationException(
          MessageKeys.BUSINESS_FIELD_REQUIRED, new MessageArgument(labelKey));
    }
    return result;
  }

  /**
   * Requires a bounded page index for callers outside the JSP layer as well.
   *
   * @param page the zero-based page index
   * @return the validated zero-based page index
   * @throws BusinessValidationException if the page index is negative or exceeds the configured
   *     maximum
   */
  public static int requirePageIndex(int page) {
    if (page < 0 || page > ValidationLimits.MAX_PAGE) {
      throw new BusinessValidationException(MessageKeys.VALIDATION_PAGE);
    }
    return page;
  }
}
