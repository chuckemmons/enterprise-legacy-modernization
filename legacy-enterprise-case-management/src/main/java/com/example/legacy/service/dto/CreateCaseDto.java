package com.example.legacy.service.dto;

/**
 * Immutable input for the CreateCaseDto application operation.
 *
 * @author Chuck Emmons
 */
public final class CreateCaseDto {
  private final String title;
  private final String description;
  private final String category;
  private final String priority;
  private final String department;
  private final java.util.Date dueAt;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CreateCaseDto(Builder builder) {
    this.title = builder.title;
    this.description = builder.description;
    this.category = builder.category;
    this.priority = builder.priority;
    this.department = builder.department;
    this.dueAt = builder.dueAt == null ? null : new java.util.Date(builder.dueAt.getTime());
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
   * Returns the category input.
   *
   * @return the category input
   */
  public String getCategory() {
    return category;
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
   * Returns the department input.
   *
   * @return the department input
   */
  public String getDepartment() {
    return department;
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
  public static Builder builder(CreateCaseDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder()
        .title(source.getTitle())
        .description(source.getDescription())
        .category(source.getCategory())
        .priority(source.getPriority())
        .department(source.getDepartment())
        .dueAt(source.getDueAt());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private String title;
    private String description;
    private String category;
    private String priority;
    private String department;
    private java.util.Date dueAt;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

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
     * Supplies the category input.
     *
     * @param value the category to set
     * @return this builder for method chaining
     */
    public Builder category(String value) {
      this.category = value;
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
     * Supplies the department input.
     *
     * @param value the department to set
     * @return this builder for method chaining
     */
    public Builder department(String value) {
      this.department = value;
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
    public CreateCaseDto build() {
      return new CreateCaseDto(this);
    }
  }
}
