package com.example.characterization;

/**
 * Provides Hibernate 3 boolean literals compatible with H2 2.x for tests.
 *
 * @author Chuck Emmons
 */
public final class H2LegacyDialect extends org.hibernate.dialect.H2Dialect {
  /**
   * Formats a boolean as an H2-compatible SQL literal.
   *
   * @param value the boolean value to format
   * @return {@code true} or {@code false}
   */
  @Override
  public String toBooleanValueString(boolean value) {
    return Boolean.toString(value);
  }
}
