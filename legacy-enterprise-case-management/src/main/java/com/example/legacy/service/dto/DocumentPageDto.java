package com.example.legacy.service.dto;

import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class DocumentPageDto extends PageDto {
  private final DocumentDto document;
  private final int declaredSize;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private DocumentPageDto(Builder builder) {
    super(builder);
    this.document = builder.document;
    this.declaredSize = builder.declaredSize;
  }

  /**
   * Returns the document.
   *
   * @return the document
   */
  public DocumentDto getDocument() {
    return document;
  }

  /**
   * Returns the declared size.
   *
   * @return the declared size
   */
  public int getDeclaredSize() {
    return declaredSize;
  }

  /**
   * Starts building a new immutable DocumentPageDto.
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
  public static Builder builder(DocumentPageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private DocumentDto document;
    private int declaredSize;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(DocumentPageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.document = source.document;
      this.declaredSize = source.declaredSize;
    }

    /**
     * Sets the builder's document value without changing any built DTO.
     *
     * @param value the document to set
     * @return this builder for method chaining
     */
    public Builder document(DocumentDto value) {
      this.document = value;
      return this;
    }

    /**
     * Sets the builder's declaredSize value without changing any built DTO.
     *
     * @param value the declared size to set
     * @return this builder for method chaining
     */
    public Builder declaredSize(int value) {
      this.declaredSize = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    @Override
    public DocumentPageDto build() {
      return new DocumentPageDto(this);
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
