package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class CaseNoteDto {
  private final Long id;
  private final Long caseId;
  private final String author;
  private final String body;
  private final Date createdAt;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CaseNoteDto(Builder builder) {
    this.id = builder.id;
    this.caseId = builder.caseId;
    this.author = builder.author;
    this.body = builder.body;
    this.createdAt = builder.createdAt == null ? null : new Date(builder.createdAt.getTime());
  }

  /**
   * Returns the record identifier.
   *
   * @return the record identifier
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the case identifier.
   *
   * @return the case identifier
   */
  public Long getCaseId() {
    return caseId;
  }

  /**
   * Returns the author.
   *
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Returns the body.
   *
   * @return the body
   */
  public String getBody() {
    return body;
  }

  /**
   * Returns a defensive copy of createdAt.
   *
   * @return a defensive copy of createdAt
   */
  public Date getCreatedAt() {
    return createdAt == null ? null : new Date(createdAt.getTime());
  }

  /**
   * Starts building a new immutable CaseNoteDto.
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
  public static Builder builder(CaseNoteDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private Long id;
    private Long caseId;
    private String author;
    private String body;
    private Date createdAt;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CaseNoteDto source) {
      this.id = source.id;
      this.caseId = source.caseId;
      this.author = source.author;
      this.body = source.body;
      this.createdAt = source.createdAt == null ? null : new Date(source.createdAt.getTime());
    }

    /**
     * Sets the builder's id value without changing any built DTO.
     *
     * @param value the record identifier to set
     * @return this builder for method chaining
     */
    public Builder id(Long value) {
      this.id = value;
      return this;
    }

    /**
     * Sets the builder's caseId value without changing any built DTO.
     *
     * @param value the case identifier to set
     * @return this builder for method chaining
     */
    public Builder caseId(Long value) {
      this.caseId = value;
      return this;
    }

    /**
     * Sets the builder's author value without changing any built DTO.
     *
     * @param value the author to set
     * @return this builder for method chaining
     */
    public Builder author(String value) {
      this.author = value;
      return this;
    }

    /**
     * Sets the builder's body value without changing any built DTO.
     *
     * @param value the body to set
     * @return this builder for method chaining
     */
    public Builder body(String value) {
      this.body = value;
      return this;
    }

    /**
     * Sets the builder's createdAt value without changing any built DTO.
     *
     * @param value the creation time to set
     * @return this builder for method chaining
     */
    public Builder createdAt(Date value) {
      this.createdAt = value == null ? null : new Date(value.getTime());
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CaseNoteDto build() {
      return new CaseNoteDto(this);
    }
  }
}
