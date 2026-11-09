package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class AuditEventDto {
  private final Long id;
  private final String entityType;
  private final Long entityId;
  private final String actionName;
  private final String actor;
  private final String details;
  private final Date createdAt;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AuditEventDto(Builder builder) {
    this.id = builder.id;
    this.entityType = builder.entityType;
    this.entityId = builder.entityId;
    this.actionName = builder.actionName;
    this.actor = builder.actor;
    this.details = builder.details;
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
   * Returns the entity type.
   *
   * @return the entity type
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Returns the entity ID.
   *
   * @return the entity ID
   */
  public Long getEntityId() {
    return entityId;
  }

  /**
   * Returns the action name.
   *
   * @return the action name
   */
  public String getActionName() {
    return actionName;
  }

  /**
   * Returns the actor.
   *
   * @return the actor
   */
  public String getActor() {
    return actor;
  }

  /**
   * Returns the details.
   *
   * @return the details
   */
  public String getDetails() {
    return details;
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
   * Starts building a new immutable AuditEventDto.
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
  public static Builder builder(AuditEventDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private Long id;
    private String entityType;
    private Long entityId;
    private String actionName;
    private String actor;
    private String details;
    private Date createdAt;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(AuditEventDto source) {
      this.id = source.id;
      this.entityType = source.entityType;
      this.entityId = source.entityId;
      this.actionName = source.actionName;
      this.actor = source.actor;
      this.details = source.details;
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
     * Sets the builder's entityType value without changing any built DTO.
     *
     * @param value the entity type to set
     * @return this builder for method chaining
     */
    public Builder entityType(String value) {
      this.entityType = value;
      return this;
    }

    /**
     * Sets the builder's entityId value without changing any built DTO.
     *
     * @param value the entity ID to set
     * @return this builder for method chaining
     */
    public Builder entityId(Long value) {
      this.entityId = value;
      return this;
    }

    /**
     * Sets the builder's actionName value without changing any built DTO.
     *
     * @param value the action name to set
     * @return this builder for method chaining
     */
    public Builder actionName(String value) {
      this.actionName = value;
      return this;
    }

    /**
     * Sets the builder's actor value without changing any built DTO.
     *
     * @param value the actor to set
     * @return this builder for method chaining
     */
    public Builder actor(String value) {
      this.actor = value;
      return this;
    }

    /**
     * Sets the builder's details value without changing any built DTO.
     *
     * @param value the details to set
     * @return this builder for method chaining
     */
    public Builder details(String value) {
      this.details = value;
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
    public AuditEventDto build() {
      return new AuditEventDto(this);
    }
  }
}
