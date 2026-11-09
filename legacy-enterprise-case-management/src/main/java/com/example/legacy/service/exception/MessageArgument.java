package com.example.legacy.service.exception;

/**
 * Marks a formatting argument as another resource key, such as a localized field label.
 *
 * @author Chuck Emmons
 */
public final class MessageArgument {
  private final String key;

  /**
   * Captures a server-selected resource identifier.
   *
   * @param key the message resource key
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public MessageArgument(String key) {
    this.key = java.util.Objects.requireNonNull(key, "key");
  }

  /**
   * Returns the resource identifier to resolve in the presentation locale.
   *
   * @return the resource identifier to resolve in the presentation locale
   */
  public String getKey() {
    return key;
  }
}
