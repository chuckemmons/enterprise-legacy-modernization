package com.example.legacy.service.dto;

/**
 * Immutable input for the CaseSearchCriteriaDto application operation.
 *
 * @author Chuck Emmons
 */
public final class CaseSearchCriteriaDto {
  private final String query;
  private final String status;
  private final String assignee;
  private final int page;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CaseSearchCriteriaDto(Builder builder) {
    this.query = builder.query;
    this.status = builder.status;
    this.assignee = builder.assignee;
    this.page = builder.page;
  }

  /**
   * Returns the search query.
   *
   * @return the search query
   */
  public String getQuery() {
    return query;
  }

  /**
   * Returns the status input.
   *
   * @return the status input
   */
  public String getStatus() {
    return status;
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
   * Returns the page input.
   *
   * @return the page input
   */
  public int getPage() {
    return page;
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
  public static Builder builder(CaseSearchCriteriaDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder()
        .query(source.getQuery())
        .status(source.getStatus())
        .assignee(source.getAssignee())
        .page(source.getPage());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private String query;
    private String status;
    private String assignee;
    private int page;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

    /**
     * Supplies the search query.
     *
     * @param value the search query to set
     * @return this builder for method chaining
     */
    public Builder query(String value) {
      this.query = value;
      return this;
    }

    /**
     * Supplies the status input.
     *
     * @param value the status to set
     * @return this builder for method chaining
     */
    public Builder status(String value) {
      this.status = value;
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
     * Supplies the page input.
     *
     * @param value the page to set
     * @return this builder for method chaining
     */
    public Builder page(int value) {
      this.page = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CaseSearchCriteriaDto build() {
      return new CaseSearchCriteriaDto(this);
    }
  }
}
