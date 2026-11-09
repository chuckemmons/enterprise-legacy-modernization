package com.example.legacy.service.dto;

import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class AuthDto extends PageDto {
  private final String username;
  private final String password;
  private final AccountDto account;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AuthDto(Builder builder) {
    super(builder);
    this.username = builder.username;
    this.password = builder.password;
    this.account = builder.account;
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
   * Returns the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the account.
   *
   * @return the account
   */
  public AccountDto getAccount() {
    return account;
  }

  /**
   * Starts building a new immutable AuthDto.
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
  public static Builder builder(AuthDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private String username;
    private String password;
    private AccountDto account;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(AuthDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.username = source.username;
      this.password = source.password;
      this.account = source.account;
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
     * Sets the builder's password value without changing any built DTO.
     *
     * @param value the password to set
     * @return this builder for method chaining
     */
    public Builder password(String value) {
      this.password = value;
      return this;
    }

    /**
     * Sets the builder's account value without changing any built DTO.
     *
     * @param value the account to set
     * @return this builder for method chaining
     */
    public Builder account(AccountDto value) {
      this.account = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    @Override
    public AuthDto build() {
      return new AuthDto(this);
    }

    /**
     * Returns this concrete builder for inherited common fields.
     *
     * @return this builder with its concrete type
     */
    @Override
    protected Builder self() {
      return this;
    }
  }
}
