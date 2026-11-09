package com.example.legacy.service.dto;

/**
 * Immutable pagination and record identity shared by page transport values.
 *
 * @author Chuck Emmons
 */
public abstract class PageDto {
  private final long id;
  private final int page;

  /**
   * Captures common values when a concrete page DTO is built.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  protected PageDto(Builder<?> builder) {
    this.id = builder.id;
    this.page = builder.page;
  }

  /**
   * Returns the requested record identity.
   *
   * @return the requested record identity
   */
  public final long getId() {
    return id;
  }

  /**
   * Returns the zero-based requested page.
   *
   * @return the zero-based requested page
   */
  public final int getPage() {
    return page;
  }

  /**
   * Typed common builder fields; mutability is confined to builders rather than DTO instances.
   *
   * @author Chuck Emmons
   * @param <B> the concrete builder type
   */
  public abstract static class Builder<B extends Builder<B>> {
    private long id;
    private int page;

    /** Initializes the common default values for a concrete page builder. */
    protected Builder() {}

    /**
     * Selects the requested record identity on this builder.
     *
     * @param value the record identifier to set
     * @return this builder for method chaining
     */
    public B id(long value) {
      this.id = value;
      return self();
    }

    /**
     * Selects the requested page on this builder.
     *
     * @param value the page to set
     * @return this builder for method chaining
     */
    public B page(int value) {
      this.page = value;
      return self();
    }

    /**
     * Creates an immutable concrete page DTO.
     *
     * @return a new immutable value containing this builder's fields
     */
    public abstract PageDto build();

    /**
     * Returns the concrete builder type for fluent common fields.
     *
     * @return this builder with its concrete type
     */
    protected abstract B self();
  }
}
