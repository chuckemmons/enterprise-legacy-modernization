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
public final class NotificationPageDto extends PageDto {
  private final List<NoticeDto> notices;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private NotificationPageDto(Builder builder) {
    super(builder);
    this.notices =
        builder.notices == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.notices));
  }

  /**
   * Returns the unmodifiable notices values.
   *
   * @return the unmodifiable notices values
   */
  public List<NoticeDto> getNotices() {
    return notices;
  }

  /**
   * Starts building a new immutable NotificationPageDto.
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
  public static Builder builder(NotificationPageDto source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder extends PageDto.Builder<Builder> {
    private List<NoticeDto> notices = Collections.emptyList();

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(NotificationPageDto source) {
      super.id(source.getId());
      super.page(source.getPage());
      this.notices =
          source.notices == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.notices));
    }

    /**
     * Sets the builder's notices value without changing any built DTO.
     *
     * @param value the notices to set
     * @return this builder for method chaining
     */
    public Builder notices(List<NoticeDto> value) {
      this.notices =
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
    public NotificationPageDto build() {
      return new NotificationPageDto(this);
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
