package com.example.legacy.service.exception;

/**
 * Correctable business input failure; presentation adapters resolve its message key.
 *
 * @author Chuck Emmons
 */
public final class BusinessValidationException extends ApplicationException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates an expected application failure with formatting arguments.
   *
   * @param key the message resource key
   * @param arguments the values substituted into the message pattern
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public BusinessValidationException(String key, Object... arguments) {
    super(null, key, arguments);
  }

  /**
   * Preserves a lower-layer cause alongside the application message contract.
   *
   * @param cause the underlying cause of the failure
   * @param key the message resource key
   * @param arguments the values substituted into the message pattern
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public BusinessValidationException(Throwable cause, String key, Object... arguments) {
    super(cause, key, arguments);
  }
}
