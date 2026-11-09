package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class DocumentDto {
  private final Long id;
  private final Long caseId;
  private final String originalName;
  private final String contentType;
  private final int contentSize;
  private final byte[] payload;
  private final String uploadedBy;
  private final Date createdAt;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private DocumentDto(Builder builder) {
    this.id = builder.id;
    this.caseId = builder.caseId;
    this.originalName = builder.originalName;
    this.contentType = builder.contentType;
    this.contentSize = builder.contentSize;
    this.payload = builder.payload == null ? null : builder.payload.clone();
    this.uploadedBy = builder.uploadedBy;
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
   * Returns the original name.
   *
   * @return the original name
   */
  public String getOriginalName() {
    return originalName;
  }

  /**
   * Returns the MIME content type.
   *
   * @return the MIME content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the content size.
   *
   * @return the content size
   */
  public int getContentSize() {
    return contentSize;
  }

  /**
   * Returns a defensive copy of payload.
   *
   * @return a defensive copy of payload
   */
  public byte[] getPayload() {
    return payload == null ? null : payload.clone();
  }

  /**
   * Returns the uploaded by.
   *
   * @return the uploaded by
   */
  public String getUploadedBy() {
    return uploadedBy;
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
   * Starts building a new immutable DocumentDto.
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
  public static Builder builder(DocumentDto source) {
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
    private String originalName;
    private String contentType;
    private int contentSize;
    private byte[] payload;
    private String uploadedBy;
    private Date createdAt;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(DocumentDto source) {
      this.id = source.id;
      this.caseId = source.caseId;
      this.originalName = source.originalName;
      this.contentType = source.contentType;
      this.contentSize = source.contentSize;
      this.payload = source.payload == null ? null : source.payload.clone();
      this.uploadedBy = source.uploadedBy;
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
     * Sets the builder's originalName value without changing any built DTO.
     *
     * @param value the original name to set
     * @return this builder for method chaining
     */
    public Builder originalName(String value) {
      this.originalName = value;
      return this;
    }

    /**
     * Sets the builder's contentType value without changing any built DTO.
     *
     * @param value the MIME content type to set
     * @return this builder for method chaining
     */
    public Builder contentType(String value) {
      this.contentType = value;
      return this;
    }

    /**
     * Sets the builder's contentSize value without changing any built DTO.
     *
     * @param value the content size to set
     * @return this builder for method chaining
     */
    public Builder contentSize(int value) {
      this.contentSize = value;
      return this;
    }

    /**
     * Sets the builder's payload value without changing any built DTO.
     *
     * @param value the payload to set
     * @return this builder for method chaining
     */
    public Builder payload(byte[] value) {
      this.payload = value == null ? null : value.clone();
      return this;
    }

    /**
     * Sets the builder's uploadedBy value without changing any built DTO.
     *
     * @param value the uploaded by to set
     * @return this builder for method chaining
     */
    public Builder uploadedBy(String value) {
      this.uploadedBy = value;
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
    public DocumentDto build() {
      return new DocumentDto(this);
    }
  }
}
