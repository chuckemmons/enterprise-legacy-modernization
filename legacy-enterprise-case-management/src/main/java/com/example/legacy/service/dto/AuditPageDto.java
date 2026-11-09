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
public final class AuditPageDto extends PageDto {
  private final List<AuditEventDto> events;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private AuditPageDto(Builder builder) {
    super(builder);
    this.events =
        builder.events == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.events));
  }

  /**
   * Returns the unmodifiable events values.
   *
   * @return the unmodifiable events values
   */
  public List<AuditEventDto> getEvents() {
    return events;
  }

  /**
   * Starts building a new immutable AuditPageDto.
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
  public static Builder builder(AuditPageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private List<AuditEventDto> events = Collections.emptyList();

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(AuditPageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.events =
          source.events == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.events));
    }

    /**
     * Sets the builder's events value without changing any built DTO.
     *
     * @param value the events to set
     * @return this builder for method chaining
     */
    public Builder events(List<AuditEventDto> value) {
      this.events =
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
    public AuditPageDto build() {
      return new AuditPageDto(this);
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
