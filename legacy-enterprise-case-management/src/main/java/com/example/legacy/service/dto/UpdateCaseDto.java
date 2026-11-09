package com.example.legacy.service.dto;

/**
 * Immutable input for the UpdateCaseDto application operation.
 *
 * @author Chuck Emmons
 */
public final class UpdateCaseDto {
  private final long id;
  private final Integer version;
  private final String title;
  private final String description;
  private final String priority;
  private final java.util.Date dueAt;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private UpdateCaseDto(Builder builder) {
    this.id = builder.id;
    this.version = builder.version;
    this.title = builder.title;
    this.description = builder.description;
    this.priority = builder.priority;
    this.dueAt = builder.dueAt == null ? null : new java.util.Date(builder.dueAt.getTime());
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
   * Returns the version input.
   *
   * @return the version input
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Returns the title input.
   *
   * @return the title input
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the description input.
   *
   * @return the description input
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the priority input.
   *
   * @return the priority input
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Returns the dueAt input.
   *
   * @return the dueAt input
   */
  public java.util.Date getDueAt() {
    return dueAt == null ? null : new java.util.Date(dueAt.getTime());
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
  public static Builder builder(UpdateCaseDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder()
        .id(source.getId())
        .version(source.getVersion())
        .title(source.getTitle())
        .description(source.getDescription())
        .priority(source.getPriority())
        .dueAt(source.getDueAt());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long id;
    private Integer version;
    private String title;
    private String description;
    private String priority;
    private java.util.Date dueAt;

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
     * Supplies the version input.
     *
     * @param value the optimistic-lock version to set
     * @return this builder for method chaining
     */
    public Builder version(Integer value) {
      this.version = value;
      return this;
    }

    /**
     * Supplies the title input.
     *
     * @param value the title to set
     * @return this builder for method chaining
     */
    public Builder title(String value) {
      this.title = value;
      return this;
    }

    /**
     * Supplies the description input.
     *
     * @param value the description to set
     * @return this builder for method chaining
     */
    public Builder description(String value) {
      this.description = value;
      return this;
    }

    /**
     * Supplies the priority input.
     *
     * @param value the priority to set
     * @return this builder for method chaining
     */
    public Builder priority(String value) {
      this.priority = value;
      return this;
    }

    /**
     * Supplies the dueAt input.
     *
     * @param value the due date to set
     * @return this builder for method chaining
     */
    public Builder dueAt(java.util.Date value) {
      this.dueAt = value == null ? null : new java.util.Date(value.getTime());
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public UpdateCaseDto build() {
      return new UpdateCaseDto(this);
    }
  }
}
