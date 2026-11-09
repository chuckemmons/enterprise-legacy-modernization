package com.example.legacy.domain;

/**
 * Defines the application's CaseStatus codes; names retain the existing persisted/submitted values.
 *
 * @author Chuck Emmons
 */
public enum CaseStatus {
  /** The new case status. */
  NEW,
  /** The triaged case status. */
  TRIAGED,
  /** The in progress case status. */
  IN_PROGRESS,
  /** The pending approval case status. */
  PENDING_APPROVAL,
  /** The approved case status. */
  APPROVED,
  /** The rejected case status. */
  REJECTED,
  /** The closed case status. */
  CLOSED;

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
