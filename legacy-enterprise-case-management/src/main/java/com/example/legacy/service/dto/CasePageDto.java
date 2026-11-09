package com.example.legacy.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class CasePageDto extends PageDto {
  private final String query;
  private final String status;
  private final String assignee;
  private final String destination;
  private final String body;
  private final CaseDto item;
  private final List<CaseDto> cases;
  private final CaseDetails details;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CasePageDto(Builder builder) {
    super(builder);
    this.query = builder.query;
    this.status = builder.status;
    this.assignee = builder.assignee;
    this.destination = builder.destination;
    this.body = builder.body;
    this.item = builder.item;
    this.cases =
        builder.cases == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.cases));
    this.details = builder.details;
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
   * Returns the status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Returns the assignee.
   *
   * @return the assignee
   */
  public String getAssignee() {
    return assignee;
  }

  /**
   * Returns the destination.
   *
   * @return the destination
   */
  public String getDestination() {
    return destination;
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
   * Returns the item.
   *
   * @return the item
   */
  public CaseDto getItem() {
    return item;
  }

  /**
   * Returns the unmodifiable cases values.
   *
   * @return the unmodifiable cases values
   */
  public List<CaseDto> getCases() {
    return cases;
  }

  /**
   * Returns the details.
   *
   * @return the details
   */
  public CaseDetails getDetails() {
    return details;
  }

  /**
   * Starts building a new immutable CasePageDto.
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
  public static Builder builder(CasePageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private String query;
    private String status;
    private String assignee;
    private String destination;
    private String body;
    private CaseDto item;
    private List<CaseDto> cases = Collections.emptyList();
    private CaseDetails details;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CasePageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.query = source.query;
      this.status = source.status;
      this.assignee = source.assignee;
      this.destination = source.destination;
      this.body = source.body;
      this.item = source.item;
      this.cases =
          source.cases == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.cases));
      this.details = source.details;
    }

    /**
     * Sets the builder's search query without changing any built DTO.
     *
     * @param value the search query to set
     * @return this builder for method chaining
     */
    public Builder query(String value) {
      this.query = value;
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
     * Sets the builder's assignee value without changing any built DTO.
     *
     * @param value the assignee to set
     * @return this builder for method chaining
     */
    public Builder assignee(String value) {
      this.assignee = value;
      return this;
    }

    /**
     * Sets the builder's destination value without changing any built DTO.
     *
     * @param value the destination to set
     * @return this builder for method chaining
     */
    public Builder destination(String value) {
      this.destination = value;
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
     * Sets the builder's item value without changing any built DTO.
     *
     * @param value the item to set
     * @return this builder for method chaining
     */
    public Builder item(CaseDto value) {
      this.item = value;
      return this;
    }

    /**
     * Sets the builder's cases value without changing any built DTO.
     *
     * @param value the cases to set
     * @return this builder for method chaining
     */
    public Builder cases(List<CaseDto> value) {
      this.cases =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's details value without changing any built DTO.
     *
     * @param value the details to set
     * @return this builder for method chaining
     */
    public Builder details(CaseDetails value) {
      this.details = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    @Override
    public CasePageDto build() {
      return new CasePageDto(this);
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
