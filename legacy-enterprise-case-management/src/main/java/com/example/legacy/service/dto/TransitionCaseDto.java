package com.example.legacy.service.dto;

/**
 * Immutable input for the TransitionCaseDto application operation.
 *
 * @author Chuck Emmons
 */
public final class TransitionCaseDto {
  private final long id;
  private final String destination;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private TransitionCaseDto(Builder builder) {
    this.id = builder.id;
    this.destination = builder.destination;
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
   * Returns the destination input.
   *
   * @return the destination input
   */
  public String getDestination() {
    return destination;
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
  public static Builder builder(TransitionCaseDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder().id(source.getId()).destination(source.getDestination());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long id;
    private String destination;

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
     * Supplies the destination input.
     *
     * @param value the destination to set
     * @return this builder for method chaining
     */
    public Builder destination(String value) {
      this.destination = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public TransitionCaseDto build() {
      return new TransitionCaseDto(this);
    }
  }
}
