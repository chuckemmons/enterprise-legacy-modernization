package com.example.legacy.service.dto;

/**
 * Immutable input for the PageRequestDto application operation.
 *
 * @author Chuck Emmons
 */
public final class PageRequestDto {
  private final int page;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private PageRequestDto(Builder builder) {
    this.page = builder.page;
  }

  /**
   * Returns the page input.
   *
   * @return the page input
   */
  public int getPage() {
    return page;
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
  public static Builder builder(PageRequestDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder().page(source.getPage());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private int page;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

    /**
     * Supplies the page input.
     *
     * @param value the page to set
     * @return this builder for method chaining
     */
    public Builder page(int value) {
      this.page = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public PageRequestDto build() {
      return new PageRequestDto(this);
    }
  }
}
