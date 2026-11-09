package com.example.legacy.domain;

/**
 * Whitelisted Hibernate properties that operational reporting may group by.
 *
 * @author Chuck Emmons
 */
public enum ReportGrouping {
  /** The status report grouping. */
  STATUS("status"),
  /** The priority report grouping. */
  PRIORITY("priority");
  private final String property;

  /**
   * Stores a server-selected mapped property name.
   *
   * @param property the property name to inspect
   */
  ReportGrouping(String property) {
    this.property = property;
  }

  /**
   * Returns the mapped property used in the DAO's grouping query.
   *
   * @return the mapped property used in the DAO's grouping query
   */
  public String getProperty() {
    return property;
  }
}
