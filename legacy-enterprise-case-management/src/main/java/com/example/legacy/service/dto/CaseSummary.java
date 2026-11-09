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
public final class CaseSummary {
  private final List<CountDto> statuses;
  private final List<CountDto> priorities;
  private final Long overdue;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CaseSummary(Builder builder) {
    this.statuses =
        builder.statuses == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.statuses));
    this.priorities =
        builder.priorities == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.priorities));
    this.overdue = builder.overdue;
  }

  /**
   * Returns the unmodifiable statuses values.
   *
   * @return the unmodifiable statuses values
   */
  public List<CountDto> getStatuses() {
    return statuses;
  }

  /**
   * Returns the unmodifiable priorities values.
   *
   * @return the unmodifiable priorities values
   */
  public List<CountDto> getPriorities() {
    return priorities;
  }

  /**
   * Returns the overdue.
   *
   * @return the overdue
   */
  public Long getOverdue() {
    return overdue;
  }

  /**
   * Starts building a new immutable CaseSummary.
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
  public static Builder builder(CaseSummary source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private List<CountDto> statuses = Collections.emptyList();
    private List<CountDto> priorities = Collections.emptyList();
    private Long overdue;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CaseSummary source) {
      this.statuses =
          source.statuses == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.statuses));
      this.priorities =
          source.priorities == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.priorities));
      this.overdue = source.overdue;
    }

    /**
     * Sets the builder's statuses value without changing any built DTO.
     *
     * @param value the statuses to set
     * @return this builder for method chaining
     */
    public Builder statuses(List<CountDto> value) {
      this.statuses =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's priorities value without changing any built DTO.
     *
     * @param value the priorities to set
     * @return this builder for method chaining
     */
    public Builder priorities(List<CountDto> value) {
      this.priorities =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's overdue value without changing any built DTO.
     *
     * @param value the overdue to set
     * @return this builder for method chaining
     */
    public Builder overdue(Long value) {
      this.overdue = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CaseSummary build() {
      return new CaseSummary(this);
    }
  }
}
