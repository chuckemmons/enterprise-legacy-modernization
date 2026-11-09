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
public final class ReportPageDto extends PageDto {
  private final CaseSummary summary;
  private final List<CaseDto> cases;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private ReportPageDto(Builder builder) {
    super(builder);
    this.summary = builder.summary;
    this.cases =
        builder.cases == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.cases));
  }

  /**
   * Returns the summary.
   *
   * @return the summary
   */
  public CaseSummary getSummary() {
    return summary;
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
   * Starts building a new immutable ReportPageDto.
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
  public static Builder builder(ReportPageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private CaseSummary summary;
    private List<CaseDto> cases = Collections.emptyList();

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(ReportPageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.summary = source.summary;
      this.cases =
          source.cases == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.cases));
    }

    /**
     * Sets the builder's summary value without changing any built DTO.
     *
     * @param value the summary to set
     * @return this builder for method chaining
     */
    public Builder summary(CaseSummary value) {
      this.summary = value;
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
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    @Override
    public ReportPageDto build() {
      return new ReportPageDto(this);
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
