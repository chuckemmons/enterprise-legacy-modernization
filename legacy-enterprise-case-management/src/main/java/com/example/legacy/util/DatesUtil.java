package com.example.legacy.util;

import com.example.legacy.service.exception.BusinessValidationException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Converts date-only values using the server time zone, with due dates set to 23:59.
 *
 * @author Chuck Emmons
 */
public final class DatesUtil {
  /** Shared value for input pattern. */
  public static final String INPUT_PATTERN = "yyyy-MM-dd";

  /** Prevents instantiation of this static utility class. */
  private DatesUtil() {}

  /**
   * Parses an ISO date at 23:59 in the server time zone; blank input returns {@code null} and
   * invalid input raises BusinessValidationException.
   *
   * @param text the date text to parse
   * @return the parsed due date at 23:59 in the server time zone, or {@code null} for blank input
   * @throws BusinessValidationException if nonblank input is not a valid ISO date
   */
  public static Date parseDueDate(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }

    try {
      return Date.from(
          LocalDate.parse(text).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant());
    } catch (DateTimeParseException failure) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_DUE_DATE_MUST_BE_YYYY_MM_DD);
    }
  }

  /**
   * Formats a timestamp as an ISO date in the server time zone, returning an empty string for
   * {@code null}.
   *
   * @param value the date value to format or copy
   * @return the ISO date text, or an empty string if the date is {@code null}
   */
  public static String formatDateInput(Date value) {
    if (value == null) {
      return "";
    }
    return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
  }
}
