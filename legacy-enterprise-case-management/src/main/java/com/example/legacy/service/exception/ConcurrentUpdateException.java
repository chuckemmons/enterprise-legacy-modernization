package com.example.legacy.service.exception;

/**
 * Optimistic locking or submitted-version conflict; presentation adapters resolve its message key.
 *
 * @author Chuck Emmons
 */
public final class ConcurrentUpdateException extends ApplicationException {
  private static final long serialVersionUID = 1L;

  /**
   * Creates an expected application failure with formatting arguments.
   *
   * @param key the message resource key
   * @param arguments the values substituted into the message pattern
   * @throws NullPointerException if {@code key} is {@code null}
   */
  public ConcurrentUpdateException(String key, Object... arguments) {
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
  public ConcurrentUpdateException(Throwable cause, String key, Object... arguments) {
    super(cause, key, arguments);
  }
}
