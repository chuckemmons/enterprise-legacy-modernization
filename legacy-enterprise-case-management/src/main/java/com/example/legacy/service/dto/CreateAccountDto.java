package com.example.legacy.service.dto;

/**
 * Immutable input for the CreateAccountDto application operation.
 *
 * @author Chuck Emmons
 */
public final class CreateAccountDto {
  private final String username;
  private final String password;
  private final String displayName;
  private final String email;
  private final String role;
  private final String department;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CreateAccountDto(Builder builder) {
    this.username = builder.username;
    this.password = builder.password;
    this.displayName = builder.displayName;
    this.email = builder.email;
    this.role = builder.role;
    this.department = builder.department;
  }

  /**
   * Returns the username input.
   *
   * @return the username input
   */
  public String getUsername() {
    return username;
  }

  /**
   * Returns the password input.
   *
   * @return the password input
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the displayName input.
   *
   * @return the displayName input
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the email input.
   *
   * @return the email input
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns the role input.
   *
   * @return the role input
   */
  public String getRole() {
    return role;
  }

  /**
   * Returns the department input.
   *
   * @return the department input
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Starts a new command builder.
   *
   * @return a new builder initialized with default values
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Starts a builder from an immutable command snapshot.
   *
   * @param source the source value to copy or map
   * @return a new builder initialized from the supplied snapshot
   * @throws NullPointerException if {@code source} is {@code null}
   */
  public static Builder builder(CreateAccountDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder()
        .username(source.getUsername())
        .password(source.getPassword())
        .displayName(source.getDisplayName())
        .email(source.getEmail())
        .role(source.getRole())
        .department(source.getDepartment());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private String username;
    private String password;
    private String displayName;
    private String email;
    private String role;
    private String department;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

    /**
     * Supplies the username input.
     *
     * @param value the username to set
     * @return this builder for method chaining
     */
    public Builder username(String value) {
      this.username = value;
      return this;
    }

    /**
     * Supplies the password input.
     *
     * @param value the password to set
     * @return this builder for method chaining
     */
    public Builder password(String value) {
      this.password = value;
      return this;
    }

    /**
     * Supplies the displayName input.
     *
     * @param value the display name to set
     * @return this builder for method chaining
     */
    public Builder displayName(String value) {
      this.displayName = value;
      return this;
    }

    /**
     * Supplies the email input.
     *
     * @param value the email to set
     * @return this builder for method chaining
     */
    public Builder email(String value) {
      this.email = value;
      return this;
    }

    /**
     * Supplies the role input.
     *
     * @param value the role to set
     * @return this builder for method chaining
     */
    public Builder role(String value) {
      this.role = value;
      return this;
    }

    /**
     * Supplies the department input.
     *
     * @param value the department to set
     * @return this builder for method chaining
     */
    public Builder department(String value) {
      this.department = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CreateAccountDto build() {
      return new CreateAccountDto(this);
    }
  }
}
