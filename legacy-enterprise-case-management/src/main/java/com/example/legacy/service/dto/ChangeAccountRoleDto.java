package com.example.legacy.service.dto;

/**
 * Immutable input for the ChangeAccountRoleDto application operation.
 *
 * @author Chuck Emmons
 */
public final class ChangeAccountRoleDto {
  private final long id;
  private final String role;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private ChangeAccountRoleDto(Builder builder) {
    this.id = builder.id;
    this.role = builder.role;
  }

  /**
   * Returns the id input.
   *
   * @return the id input
   */
  public long getId() {
    return id;
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
  public static Builder builder(ChangeAccountRoleDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder().id(source.getId()).role(source.getRole());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long id;
    private String role;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

    /**
     * Supplies the id input.
     *
     * @param value the record identifier to set
     * @return this builder for method chaining
     */
    public Builder id(long value) {
      this.id = value;
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
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public ChangeAccountRoleDto build() {
      return new ChangeAccountRoleDto(this);
    }
  }
}
