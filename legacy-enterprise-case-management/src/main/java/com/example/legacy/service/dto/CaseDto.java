package com.example.legacy.service.dto;

import java.util.Date;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class CaseDto {
  private final Long id;
  private final String caseNumber;
  private final String title;
  private final String description;
  private final String category;
  private final String priority;
  private final String status;
  private final String createdBy;
  private final String assignedTo;
  private final String department;
  private final Date dueAt;
  private final Date closedAt;
  private final Date createdAt;
  private final Date updatedAt;
  private final Integer version;
  private final boolean archived;
  private final boolean overdueNoticeSent;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CaseDto(Builder builder) {
    this.id = builder.id;
    this.caseNumber = builder.caseNumber;
    this.title = builder.title;
    this.description = builder.description;
    this.category = builder.category;
    this.priority = builder.priority;
    this.status = builder.status;
    this.createdBy = builder.createdBy;
    this.assignedTo = builder.assignedTo;
    this.department = builder.department;
    this.dueAt = builder.dueAt == null ? null : new Date(builder.dueAt.getTime());
    this.closedAt = builder.closedAt == null ? null : new Date(builder.closedAt.getTime());
    this.createdAt = builder.createdAt == null ? null : new Date(builder.createdAt.getTime());
    this.updatedAt = builder.updatedAt == null ? null : new Date(builder.updatedAt.getTime());
    this.version = builder.version;
    this.archived = builder.archived;
    this.overdueNoticeSent = builder.overdueNoticeSent;
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
   * Returns the case number.
   *
   * @return the case number
   */
  public String getCaseNumber() {
    return caseNumber;
  }

  /**
   * Returns the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the category.
   *
   * @return the category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Returns the priority.
   *
   * @return the priority
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Returns the status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Returns the creator's username.
   *
   * @return the creator's username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Returns the assignee's username.
   *
   * @return the assignee's username
   */
  public String getAssignedTo() {
    return assignedTo;
  }

  /**
   * Returns the department.
   *
   * @return the department
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Returns a defensive copy of dueAt.
   *
   * @return a defensive copy of dueAt
   */
  public Date getDueAt() {
    return dueAt == null ? null : new Date(dueAt.getTime());
  }

  /**
   * Returns a defensive copy of closedAt.
   *
   * @return a defensive copy of closedAt
   */
  public Date getClosedAt() {
    return closedAt == null ? null : new Date(closedAt.getTime());
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
   * Returns a defensive copy of updatedAt.
   *
   * @return a defensive copy of updatedAt
   */
  public Date getUpdatedAt() {
    return updatedAt == null ? null : new Date(updatedAt.getTime());
  }

  /**
   * Returns the optimistic-lock version.
   *
   * @return the optimistic-lock version
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Returns the archived flag.
   *
   * @return the archived flag
   */
  public boolean isArchived() {
    return archived;
  }

  /**
   * Returns the overdue-notification flag.
   *
   * @return the overdue-notification flag
   */
  public boolean isOverdueNoticeSent() {
    return overdueNoticeSent;
  }

  /**
   * Starts building a new immutable CaseDto.
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
  public static Builder builder(CaseDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private Long id;
    private String caseNumber;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String createdBy;
    private String assignedTo;
    private String department;
    private Date dueAt;
    private Date closedAt;
    private Date createdAt;
    private Date updatedAt;
    private Integer version;
    private boolean archived;
    private boolean overdueNoticeSent;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CaseDto source) {
      this.id = source.id;
      this.caseNumber = source.caseNumber;
      this.title = source.title;
      this.description = source.description;
      this.category = source.category;
      this.priority = source.priority;
      this.status = source.status;
      this.createdBy = source.createdBy;
      this.assignedTo = source.assignedTo;
      this.department = source.department;
      this.dueAt = source.dueAt == null ? null : new Date(source.dueAt.getTime());
      this.closedAt = source.closedAt == null ? null : new Date(source.closedAt.getTime());
      this.createdAt = source.createdAt == null ? null : new Date(source.createdAt.getTime());
      this.updatedAt = source.updatedAt == null ? null : new Date(source.updatedAt.getTime());
      this.version = source.version;
      this.archived = source.archived;
      this.overdueNoticeSent = source.overdueNoticeSent;
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
     * Sets the builder's caseNumber value without changing any built DTO.
     *
     * @param value the case number to set
     * @return this builder for method chaining
     */
    public Builder caseNumber(String value) {
      this.caseNumber = value;
      return this;
    }

    /**
     * Sets the builder's title value without changing any built DTO.
     *
     * @param value the title to set
     * @return this builder for method chaining
     */
    public Builder title(String value) {
      this.title = value;
      return this;
    }

    /**
     * Sets the builder's description value without changing any built DTO.
     *
     * @param value the description to set
     * @return this builder for method chaining
     */
    public Builder description(String value) {
      this.description = value;
      return this;
    }

    /**
     * Sets the builder's category value without changing any built DTO.
     *
     * @param value the category to set
     * @return this builder for method chaining
     */
    public Builder category(String value) {
      this.category = value;
      return this;
    }

    /**
     * Sets the builder's priority value without changing any built DTO.
     *
     * @param value the priority to set
     * @return this builder for method chaining
     */
    public Builder priority(String value) {
      this.priority = value;
      return this;
    }

    /**
     * Sets the builder's status value without changing any built DTO.
     *
     * @param value the status to set
     * @return this builder for method chaining
     */
    public Builder status(String value) {
      this.status = value;
      return this;
    }

    /**
     * Sets the builder's createdBy value without changing any built DTO.
     *
     * @param value the creator's username to set
     * @return this builder for method chaining
     */
    public Builder createdBy(String value) {
      this.createdBy = value;
      return this;
    }

    /**
     * Sets the builder's assignedTo value without changing any built DTO.
     *
     * @param value the assignee's username to set
     * @return this builder for method chaining
     */
    public Builder assignedTo(String value) {
      this.assignedTo = value;
      return this;
    }

    /**
     * Sets the builder's department value without changing any built DTO.
     *
     * @param value the department to set
     * @return this builder for method chaining
     */
    public Builder department(String value) {
      this.department = value;
      return this;
    }

    /**
     * Sets the builder's dueAt value without changing any built DTO.
     *
     * @param value the due date to set
     * @return this builder for method chaining
     */
    public Builder dueAt(Date value) {
      this.dueAt = value == null ? null : new Date(value.getTime());
      return this;
    }

    /**
     * Sets the builder's closedAt value without changing any built DTO.
     *
     * @param value the closure time to set
     * @return this builder for method chaining
     */
    public Builder closedAt(Date value) {
      this.closedAt = value == null ? null : new Date(value.getTime());
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
     * Sets the builder's update time without changing any built DTO.
     *
     * @param value the last update time to set
     * @return this builder for method chaining
     */
    public Builder updatedAt(Date value) {
      this.updatedAt = value == null ? null : new Date(value.getTime());
      return this;
    }

    /**
     * Sets the builder's version value without changing any built DTO.
     *
     * @param value the optimistic-lock version to set
     * @return this builder for method chaining
     */
    public Builder version(Integer value) {
      this.version = value;
      return this;
    }

    /**
     * Sets the builder's archived value without changing any built DTO.
     *
     * @param value the archived flag to set
     * @return this builder for method chaining
     */
    public Builder archived(boolean value) {
      this.archived = value;
      return this;
    }

    /**
     * Sets the builder's overdueNoticeSent value without changing any built DTO.
     *
     * @param value the overdue-notification flag to set
     * @return this builder for method chaining
     */
    public Builder overdueNoticeSent(boolean value) {
      this.overdueNoticeSent = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CaseDto build() {
      return new CaseDto(this);
    }
  }
}
