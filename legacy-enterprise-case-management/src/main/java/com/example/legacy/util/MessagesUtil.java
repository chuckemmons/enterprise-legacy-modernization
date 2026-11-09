package com.example.legacy.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Resolves application text without introducing servlet or Struts dependencies into business code.
 *
 * @author Chuck Emmons
 */
public final class MessagesUtil {
  /** Shared value for bundle. */
  public static final String BUNDLE = "ApplicationResources";

  /** Prevents instantiation of this shared message helper. */
  private MessagesUtil() {}

  /**
   * Resolves a stable English message for business exceptions, CLI callers, and durable audit text.
   *
   * @param key the message resource key
   * @param arguments the values substituted into the message pattern
   * @return the resolved message with its arguments formatted for the selected locale
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  public static String formatMessage(String key, Object... arguments) {
    return formatMessage(Locale.ENGLISH, key, arguments);
  }

  /**
   * Formats a locale-specific message from the application's shared resources.
   *
   * @param locale the locale used to resolve and format the message
   * @param key the message resource key
   * @param arguments the values substituted into the message pattern
   * @return the resolved message with its arguments formatted for the selected locale
   * @throws IllegalArgumentException if a message pattern or formatting argument is invalid
   * @throws java.util.MissingResourceException if a required message bundle or message key cannot
   *     be found
   */
  public static String formatMessage(Locale locale, String key, Object... arguments) {
    String pattern = ResourceBundle.getBundle(BUNDLE, locale).getString(key);
    return new MessageFormat(pattern, locale).format(arguments);
  }
}
