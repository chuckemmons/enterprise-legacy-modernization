package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class NoticeDto {
  private final Long id;
  private final String recipient;
  private final String subject;
  private final String message;
  private final Long caseId;
  private final boolean read;
  private final Date sentAt;
  private final Date createdAt;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private NoticeDto(Builder builder) {
    this.id = builder.id;
    this.recipient = builder.recipient;
    this.subject = builder.subject;
    this.message = builder.message;
    this.caseId = builder.caseId;
    this.read = builder.read;
    this.sentAt = builder.sentAt == null ? null : new Date(builder.sentAt.getTime());
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
   * Returns the recipient.
   *
   * @return the recipient
   */
  public String getRecipient() {
    return recipient;
  }

  /**
   * Returns the subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Returns the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
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
   * Returns the read.
   *
   * @return the read
   */
  public boolean isRead() {
    return read;
  }

  /**
   * Returns a defensive copy of sentAt.
   *
   * @return a defensive copy of sentAt
   */
  public Date getSentAt() {
    return sentAt == null ? null : new Date(sentAt.getTime());
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
   * Starts building a new immutable NoticeDto.
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
  public static Builder builder(NoticeDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private Long id;
    private String recipient;
    private String subject;
    private String message;
    private Long caseId;
    private boolean read;
    private Date sentAt;
    private Date createdAt;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(NoticeDto source) {
      this.id = source.id;
      this.recipient = source.recipient;
      this.subject = source.subject;
      this.message = source.message;
      this.caseId = source.caseId;
      this.read = source.read;
      this.sentAt = source.sentAt == null ? null : new Date(source.sentAt.getTime());
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
     * Sets the builder's recipient value without changing any built DTO.
     *
     * @param value the recipient to set
     * @return this builder for method chaining
     */
    public Builder recipient(String value) {
      this.recipient = value;
      return this;
    }

    /**
     * Sets the builder's subject value without changing any built DTO.
     *
     * @param value the subject to set
     * @return this builder for method chaining
     */
    public Builder subject(String value) {
      this.subject = value;
      return this;
    }

    /**
     * Sets the builder's message value without changing any built DTO.
     *
     * @param value the message to set
     * @return this builder for method chaining
     */
    public Builder message(String value) {
      this.message = value;
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
     * Sets the builder's read value without changing any built DTO.
     *
     * @param value the read to set
     * @return this builder for method chaining
     */
    public Builder read(boolean value) {
      this.read = value;
      return this;
    }

    /**
     * Sets the builder's sentAt value without changing any built DTO.
     *
     * @param value the sent at to set
     * @return this builder for method chaining
     */
    public Builder sentAt(Date value) {
      this.sentAt = value == null ? null : new Date(value.getTime());
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
    public NoticeDto build() {
      return new NoticeDto(this);
    }
  }
}
