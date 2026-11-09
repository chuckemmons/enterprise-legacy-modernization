package com.example.legacy.service.dto;

/**
 * Immutable input for the AssignCaseDto application operation.
 *
 * @author Chuck Emmons
 */
public final class AssignCaseDto {
  private final long id;
  private final String assignee;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AssignCaseDto(Builder builder) {
    this.id = builder.id;
    this.assignee = builder.assignee;
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
   * Returns the assignee input.
   *
   * @return the assignee input
   */
  public String getAssignee() {
    return assignee;
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
  public static Builder builder(AssignCaseDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder().id(source.getId()).assignee(source.getAssignee());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long id;
    private String assignee;

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
     * Supplies the assignee input.
     *
     * @param value the assignee to set
     * @return this builder for method chaining
     */
    public Builder assignee(String value) {
      this.assignee = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public AssignCaseDto build() {
      return new AssignCaseDto(this);
    }
  }
}
