package com.example.legacy.domain;

/**
 * Defines the application's AuditAction codes; names retain the existing persisted/submitted
 * values.
 *
 * @author Chuck Emmons
 */
public enum AuditAction {
  /** The create audit action. */
  CREATE,
  /** The update audit action. */
  UPDATE,
  /** The assign audit action. */
  ASSIGN,
  /** The transition audit action. */
  TRANSITION,
  /** The comment audit action. */
  COMMENT,
  /** The archive audit action. */
  ARCHIVE,
  /** The upload audit action. */
  UPLOAD,
  /** The role change audit action. */
  ROLE_CHANGE,
  /** The enable audit action. */
  ENABLE;

  /**
   * Compares an external code safely, including {@code null} or unknown values.
   *
   * @param code the external code to compare
   * @return {@code true} if the supplied code matches this value; {@code false} otherwise
   */
  public boolean matchesCode(String code) {
    return name().equals(code);
  }

  /**
   * Returns a fresh array of supported codes in declaration order.
   *
   * @return a fresh array of supported codes in declaration order
   */
  public static String[] getCodes() {
    return java.util.Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
  }
}
