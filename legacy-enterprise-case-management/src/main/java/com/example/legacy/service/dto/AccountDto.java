package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class AccountDto {
  private final Long id;
  private final String username;
  private final String displayName;
  private final String email;
  private final String role;
  private final String department;
  private final boolean enabled;
  private final Date createdAt;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AccountDto(Builder builder) {
    this.id = builder.id;
    this.username = builder.username;
    this.displayName = builder.displayName;
    this.email = builder.email;
    this.role = builder.role;
    this.department = builder.department;
    this.enabled = builder.enabled;
    this.createdAt = builder.createdAt == null ? null : new Date(builder.createdAt.getTime());
  }

  /**
   * Returns the record identifier.
   *
   * @return the record identifier
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the username.
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Returns the display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the email.
   *
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns the role.
   *
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * Returns the department.
   *
   * @return the department
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Returns the enabled flag.
   *
   * @return the enabled flag
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns a defensive copy of createdAt.
   *
   * @return a defensive copy of createdAt
   */
  public Date getCreatedAt() {
    return createdAt == null ? null : new Date(createdAt.getTime());
  }

  /**
   * Starts building a new immutable AccountDto.
   *
   * @return a new builder initialized with default values
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Starts building a new value from an existing snapshot without changing it.
   *
   * @param source the source value to copy or map
   * @return a new builder initialized from the supplied snapshot
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public static Builder builder(AccountDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String role;
    private String department;
    private boolean enabled;
    private Date createdAt;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(AccountDto source) {
      this.id = source.id;
      this.username = source.username;
      this.displayName = source.displayName;
      this.email = source.email;
      this.role = source.role;
      this.department = source.department;
      this.enabled = source.enabled;
      this.createdAt = source.createdAt == null ? null : new Date(source.createdAt.getTime());
    }

    /**
     * Sets the builder's id value without changing any built DTO.
     *
     * @param value the record identifier to set
     * @return this builder for method chaining
     */
    public Builder id(Long value) {
      this.id = value;
      return this;
    }

    /**
     * Sets the builder's username value without changing any built DTO.
     *
     * @param value the username to set
     * @return this builder for method chaining
     */
    public Builder username(String value) {
      this.username = value;
      return this;
    }

    /**
     * Sets the builder's displayName value without changing any built DTO.
     *
     * @param value the display name to set
     * @return this builder for method chaining
     */
    public Builder displayName(String value) {
      this.displayName = value;
      return this;
    }

    /**
     * Sets the builder's email value without changing any built DTO.
     *
     * @param value the email to set
     * @return this builder for method chaining
     */
    public Builder email(String value) {
      this.email = value;
      return this;
    }

    /**
     * Sets the builder's role value without changing any built DTO.
     *
     * @param value the role to set
     * @return this builder for method chaining
     */
    public Builder role(String value) {
      this.role = value;
      return this;
    }

    /**
     * Sets the builder's department value without changing any built DTO.
     *
     * @param value the department to set
     * @return this builder for method chaining
     */
    public Builder department(String value) {
      this.department = value;
      return this;
    }

    /**
     * Sets the builder's enabled value without changing any built DTO.
     *
     * @param value the enabled flag to set
     * @return this builder for method chaining
     */
    public Builder enabled(boolean value) {
      this.enabled = value;
      return this;
    }

    /**
     * Sets the builder's createdAt value without changing any built DTO.
     *
     * @param value the creation time to set
     * @return this builder for method chaining
     */
    public Builder createdAt(Date value) {
      this.createdAt = value == null ? null : new Date(value.getTime());
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public AccountDto build() {
      return new AccountDto(this);
    }
  }
}
