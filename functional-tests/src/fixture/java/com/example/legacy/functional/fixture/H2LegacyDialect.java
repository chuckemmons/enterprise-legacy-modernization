package com.example.legacy.functional.fixture;

import org.hibernate.dialect.H2Dialect;

/**
 * Adapts Hibernate 3 boolean literals to H2 2's strict BOOLEAN columns in the staged WAR only.
 *
 * @author Chuck Emmons
 */
public class H2LegacyDialect extends H2Dialect {
  /**
   * Emits SQL boolean literals instead of Hibernate 3's numeric boolean representation.
   *
   * @param value the boolean value to render as SQL
   * @return {@code "true"} or {@code "false"}, matching the supplied boolean value
   */
  @Override
  public String toBooleanValueString(boolean value) {
    return Boolean.toString(value);
  }
}
