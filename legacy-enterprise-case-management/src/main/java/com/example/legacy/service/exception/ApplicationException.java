package com.example.legacy.service.exception;

/**
 * Expected application failure with a stable message identifier and immutable presentation
 * arguments.
 *
 * @author Chuck Emmons
 */
public abstract class ApplicationException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String messageKey;
  private final Object[] arguments;

  /**
   * Retains the failure category, optional cause, and scalar/resource-key arguments.
   *
   * @param cause the underlying cause of the failure
   * @param messageKey the message resource key
   * @param arguments the values substituted into the message pattern
   * @throws NullPointerException if {@code messageKey} is {@code null}
   */
  protected ApplicationException(Throwable cause, String messageKey, Object... arguments) {
    super(java.util.Objects.requireNonNull(messageKey, "messageKey"), cause);
    this.messageKey = messageKey;
    this.arguments = arguments == null ? new Object[0] : arguments.clone();
  }

  /**
   * Returns the resource key without formatting user-facing text in services.
   *
   * @return the resource key without formatting user-facing text in services
   */
  public String getMessageKey() {
    return messageKey;
  }

  /**
   * Returns a defensive copy of formatting arguments.
   *
   * @return a defensive copy of formatting arguments
   */
  public Object[] getArguments() {
    return arguments.clone();
  }
}
