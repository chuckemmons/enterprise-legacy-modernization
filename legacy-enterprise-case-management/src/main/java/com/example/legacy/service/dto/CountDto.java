package com.example.legacy.service.dto;

import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class CountDto {
  private final String label;
  private final long total;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CountDto(Builder builder) {
    this.label = builder.label;
    this.total = builder.total;
  }

  /**
   * Returns the label.
   *
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns the total.
   *
   * @return the total
   */
  public long getTotal() {
    return total;
  }

  /**
   * Starts building a new immutable CountDto.
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
  public static Builder builder(CountDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private String label;
    private long total;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CountDto source) {
      this.label = source.label;
      this.total = source.total;
    }

    /**
     * Sets the builder's label value without changing any built DTO.
     *
     * @param value the label to set
     * @return this builder for method chaining
     */
    public Builder label(String value) {
      this.label = value;
      return this;
    }

    /**
     * Sets the builder's total value without changing any built DTO.
     *
     * @param value the total to set
     * @return this builder for method chaining
     */
    public Builder total(long value) {
      this.total = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CountDto build() {
      return new CountDto(this);
    }
  }
}
