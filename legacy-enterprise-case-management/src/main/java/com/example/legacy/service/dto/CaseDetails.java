package com.example.legacy.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable transport data created through a builder, with no entity or framework dependencies.
 *
 * @author Chuck Emmons
 */
public final class CaseDetails {
  private final CaseDto item;
  private final List<CaseNoteDto> notes;
  private final List<DocumentDto> documents;
  private final List<AccountDto> users;
  private final Set<String> nextStates;
  private final boolean mayEdit;
  private final boolean mayComment;

  /**
   * Takes a defensive snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private CaseDetails(Builder builder) {
    this.item = builder.item;
    this.notes =
        builder.notes == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.notes));
    this.documents =
        builder.documents == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.documents));
    this.users =
        builder.users == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(builder.users));
    this.nextStates =
        builder.nextStates == null
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(builder.nextStates));
    this.mayEdit = builder.mayEdit;
    this.mayComment = builder.mayComment;
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
   * Returns the unmodifiable notes values.
   *
   * @return the unmodifiable notes values
   */
  public List<CaseNoteDto> getNotes() {
    return notes;
  }

  /**
   * Returns the unmodifiable documents values.
   *
   * @return the unmodifiable documents values
   */
  public List<DocumentDto> getDocuments() {
    return documents;
  }

  /**
   * Returns the unmodifiable users values.
   *
   * @return the unmodifiable users values
   */
  public List<AccountDto> getUsers() {
    return users;
  }

  /**
   * Returns the unmodifiable nextStates values.
   *
   * @return the unmodifiable nextStates values
   */
  public Set<String> getNextStates() {
    return nextStates;
  }

  /**
   * Returns the may edit.
   *
   * @return the may edit
   */
  public boolean isMayEdit() {
    return mayEdit;
  }

  /**
   * Returns the may comment.
   *
   * @return the may comment
   */
  public boolean isMayComment() {
    return mayComment;
  }

  /**
   * Starts building a new immutable CaseDetails.
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
  public static Builder builder(CaseDetails source) {
    return new Builder(Objects.requireNonNull(source, "source"));
  }

  /**
   * Mutable construction state; built DTOs never share writable values with their builder.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private CaseDto item;
    private List<CaseNoteDto> notes = Collections.emptyList();
    private List<DocumentDto> documents = Collections.emptyList();
    private List<AccountDto> users = Collections.emptyList();
    private Set<String> nextStates = Collections.emptySet();
    private boolean mayEdit;
    private boolean mayComment;

    /** Initializes default builder values. */
    private Builder() {}

    /**
     * Copies an existing immutable snapshot into independent construction state.
     *
     * @param source the source value to copy or map
     * @throws NullPointerException if {@code source} is {@code null}
     */
    private Builder(CaseDetails source) {
      this.item = source.item;
      this.notes =
          source.notes == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.notes));
      this.documents =
          source.documents == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.documents));
      this.users =
          source.users == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(source.users));
      this.nextStates =
          source.nextStates == null
              ? Collections.emptySet()
              : Collections.unmodifiableSet(new LinkedHashSet<>(source.nextStates));
      this.mayEdit = source.mayEdit;
      this.mayComment = source.mayComment;
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
     * Sets the builder's notes value without changing any built DTO.
     *
     * @param value the notes to set
     * @return this builder for method chaining
     */
    public Builder notes(List<CaseNoteDto> value) {
      this.notes =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's documents value without changing any built DTO.
     *
     * @param value the documents to set
     * @return this builder for method chaining
     */
    public Builder documents(List<DocumentDto> value) {
      this.documents =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's users value without changing any built DTO.
     *
     * @param value the users to set
     * @return this builder for method chaining
     */
    public Builder users(List<AccountDto> value) {
      this.users =
          value == null
              ? Collections.emptyList()
              : Collections.unmodifiableList(new ArrayList<>(value));
      return this;
    }

    /**
     * Sets the builder's nextStates value without changing any built DTO.
     *
     * @param value the next states to set
     * @return this builder for method chaining
     */
    public Builder nextStates(Set<String> value) {
      this.nextStates =
          value == null
              ? Collections.emptySet()
              : Collections.unmodifiableSet(new LinkedHashSet<>(value));
      return this;
    }

    /**
     * Sets the builder's mayEdit value without changing any built DTO.
     *
     * @param value the may edit to set
     * @return this builder for method chaining
     */
    public Builder mayEdit(boolean value) {
      this.mayEdit = value;
      return this;
    }

    /**
     * Sets the builder's mayComment value without changing any built DTO.
     *
     * @param value the may comment to set
     * @return this builder for method chaining
     */
    public Builder mayComment(boolean value) {
      this.mayComment = value;
      return this;
    }

    /**
     * Creates an immutable defensive snapshot of this builder.
     *
     * @return a new immutable value containing this builder's fields
     */
    public CaseDetails build() {
      return new CaseDetails(this);
    }
  }
}
