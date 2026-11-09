package com.example.legacy.integration.dao;

import org.hibernate.dialect.H2Dialect;

/**
 * Adapts Hibernate 3's numeric boolean literals to H2 2.x while retaining its sequence/pagination
 * support.
 *
 * @author Chuck Emmons
 */
public class H2LegacyDialect extends H2Dialect {
  /**
   * Emits SQL booleans accepted by current H2 PostgreSQL compatibility mode.
   *
   * @param value the boolean value to render as SQL
   * @return {@code "true"} or {@code "false"}, matching the supplied boolean value
   */
  @Override
  public String toBooleanValueString(boolean value) {
    return Boolean.toString(value);
  }
}
