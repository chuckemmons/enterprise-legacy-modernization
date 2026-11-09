package com.example.legacy.util;

import com.example.legacy.service.exception.BusinessValidationException;
import java.util.Locale;

/**
 * Normalizes and validates input strings and prepares filenames and CSV cells for output.
 *
 * @author Chuck Emmons
 */
public final class StringsUtil {
  /** Prevents instantiation of this static utility class. */
  private StringsUtil() {}

  /**
   * Converts {@code null} to empty, trims whitespace, and removes control characters except CR, LF,
   * and tab; rejects results exceeding the maximum length.
   *
   * @param text the input text
   * @param maximumLength the maximum permitted text length
   * @return the trimmed text without disallowed control characters, or an empty string for {@code
   *     null}
   * @throws BusinessValidationException if the normalized text exceeds {@code maximumLength}
   *     characters
   */
  public static String normalizeText(String text, int maximumLength) {
    if (text == null) {
      return "";
    }
    String normalizedText = text.trim().replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
    if (normalizedText.length() > maximumLength) {
      throw new BusinessValidationException(MessageKeys.BUSINESS_FIELD_TOO_LONG, maximumLength);
    }
    return normalizedText;
  }

  /**
   * Returns an exact, case-sensitive allowed value or throws BusinessValidationException for an
   * unrecognized selection.
   *
   * @param text the input text
   * @param choices the allowed case-sensitive values
   * @return the supplied value if it matches an allowed choice
   * @throws BusinessValidationException if the input does not match any allowed choice
   * @throws NullPointerException if {@code choices} is {@code null}
   */
  public static String requireAllowedChoice(String text, String... choices) {
    for (String choice : choices) {
      if (choice.equals(text)) {
        return text;
      }
    }
    throw new BusinessValidationException(MessageKeys.BUSINESS_INVALID_SELECTION);
  }

  /**
   * Removes path components, replaces unsupported filename characters, and retains at most the
   * final 120 characters; empty names become document.
   *
   * @param filename the input text
   * @return the sanitized filename, or {@code "document"} if no usable name remains
   */
  public static String sanitizeFilename(String filename) {
    String value = filename == null ? "document" : filename.replace('\\', '/');
    int i = value.lastIndexOf('/');
    if (i >= 0) {
      value = value.substring(i + 1);
    }
    value = value.replaceAll("[^a-zA-Z0-9._-]", "_");
    if (value.length() > 120) {
      value = value.substring(value.length() - 120);
    }
    return value.length() == 0 ? "document" : value;
  }

  /**
   * Quotes and escapes a CSV cell, flattens line breaks, and prefixes formula-like values with an
   * apostrophe.
   *
   * @param text the input text
   * @return the quoted CSV cell with embedded quotes escaped and formula-like input prefixed
   */
  public static String escapeCsvCell(String text) {
    String value = text == null ? "" : text.replace("\r", " ").replace("\n", " ");
    if (value.matches("^[=+@\\-\\t].*")) {
      value = "'" + value;
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  /**
   * Returns a locale-independent lowercase value, treating {@code null} as an empty string.
   *
   * @param text the input text
   * @return the lowercase text, or an empty string for {@code null}
   */
  public static String convertToLowerCase(String text) {
    return text == null ? "" : text.toLowerCase(Locale.ROOT);
  }
}
