package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped user account containing credentials, role, department, and enabled state.
 * Business validation belongs to the service layer.
 *
 * @author Chuck Emmons
 */
public class Account {
  private Long id;
  private String username;
  private String passwordHash;
  private String displayName;
  private String email;
  private String role;
  private String department;
  private boolean enabled;
  private Date createdAt;

  /**
   * Returns database-generated identifier.
   *
   * @return database-generated identifier
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets database-generated identifier.
   *
   * @param value the record identifier to set
   */
  public void setId(Long value) {
    this.id = value;
  }

  /**
   * Returns login username.
   *
   * @return login username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets login username.
   *
   * @param value the username to set
   */
  public void setUsername(String value) {
    this.username = value;
  }

  /**
   * Returns encoded salted password hash.
   *
   * @return encoded salted password hash
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * Sets encoded salted password hash.
   *
   * @param value the password hash to set
   */
  public void setPasswordHash(String value) {
    this.passwordHash = value;
  }

  /**
   * Returns user-facing display name.
   *
   * @return user-facing display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets user-facing display name.
   *
   * @param value the display name to set
   */
  public void setDisplayName(String value) {
    this.displayName = value;
  }

  /**
   * Returns email address used for notification delivery.
   *
   * @return email address used for notification delivery
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets email address used for notification delivery.
   *
   * @param value the email to set
   */
  public void setEmail(String value) {
    this.email = value;
  }

  /**
   * Returns authorization role.
   *
   * @return authorization role
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets authorization role.
   *
   * @param value the role to set
   */
  public void setRole(String value) {
    this.role = value;
  }

  /**
   * Returns department name.
   *
   * @return department name
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Sets department name.
   *
   * @param value the department to set
   */
  public void setDepartment(String value) {
    this.department = value;
  }

  /**
   * Returns whether the account is enabled for authentication and active use.
   *
   * @return whether the account is enabled for authentication and active use
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether the account is enabled for authentication and active use.
   *
   * @param value the enabled flag to set
   */
  public void setEnabled(boolean value) {
    this.enabled = value;
  }

  /**
   * Returns creation timestamp.
   *
   * @return creation timestamp
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets creation timestamp.
   *
   * @param value the creation time to set
   */
  public void setCreatedAt(Date value) {
    this.createdAt = value;
  }
}
