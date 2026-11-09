package com.example.legacy.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class UserPageDto extends PageDto {
  private final AccountDto account;
  private final String password;
  private final List<AccountDto> users;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private UserPageDto(Builder builder) {
    super(builder);
    this.account = builder.account;
    this.password = builder.password;
    this.users =
        builder.users == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.users));
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
   * Returns the password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the unmodifiable users values.
   *
   * @return the unmodifiable users values
   */
  public List<AccountDto> getUsers() {
    return users;
  }

  /**
   * Starts building a new immutable UserPageDto.
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
  public static Builder builder(UserPageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private AccountDto account;
    private String password;
    private List<AccountDto> users = Collections.emptyList();

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(UserPageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.account = source.account;
      this.password = source.password;
      this.users =
          source.users == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.users));
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
     * Sets the builder's users value without changing any built DTO.
     *
     * @param value the users to set
     * @return this builder for method chaining
     */
    public Builder users(List<AccountDto> value) {
      this.users =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    @Override
    public UserPageDto build() {
      return new UserPageDto(this);
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
