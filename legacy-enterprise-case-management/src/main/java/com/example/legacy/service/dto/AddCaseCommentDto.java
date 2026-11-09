package com.example.legacy.service.dto;

/**
 * Immutable input for the AddCaseCommentDto application operation.
 *
 * @author Chuck Emmons
 */
public final class AddCaseCommentDto {
  private final long id;
  private final String body;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AddCaseCommentDto(Builder builder) {
    this.id = builder.id;
    this.body = builder.body;
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
   * Returns the body input.
   *
   * @return the body input
   */
  public String getBody() {
    return body;
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
  public static Builder builder(AddCaseCommentDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder().id(source.getId()).body(source.getBody());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long id;
    private String body;

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
     * Supplies the body input.
     *
     * @param value the body to set
     * @return this builder for method chaining
     */
    public Builder body(String value) {
      this.body = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public AddCaseCommentDto build() {
      return new AddCaseCommentDto(this);
    }
  }
}
