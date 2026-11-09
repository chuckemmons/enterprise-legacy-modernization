package com.example.legacy.service.dto;

/**
 * Immutable input for the UploadDocumentDto application operation.
 *
 * @author Chuck Emmons
 */
public final class UploadDocumentDto {
  private final long caseId;
  private final DocumentDto document;
  private final int declaredSize;

  /**
   * Captures an independent snapshot of builder values.
   *
   * @param builder the builder containing the initial values
   * @throws NullPointerException if {@code builder} is {@code null}
   */
  private UploadDocumentDto(Builder builder) {
    this.caseId = builder.caseId;
    this.document = builder.document;
    this.declaredSize = builder.declaredSize;
  }

  /**
   * Returns the caseId input.
   *
   * @return the caseId input
   */
  public long getCaseId() {
    return caseId;
  }

  /**
   * Returns the document input.
   *
   * @return the document input
   */
  public DocumentDto getDocument() {
    return document;
  }

  /**
   * Returns the declaredSize input.
   *
   * @return the declaredSize input
   */
  public int getDeclaredSize() {
    return declaredSize;
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
  public static Builder builder(UploadDocumentDto source) {
    java.util.Objects.requireNonNull(source, "source");
    return new Builder()
        .caseId(source.getCaseId())
        .document(source.getDocument())
        .declaredSize(source.getDeclaredSize());
  }

  /**
   * Mutable command assembly kept outside the immutable DTO API.
   *
   * @author Chuck Emmons
   */
  public static final class Builder {
    private long caseId;
    private DocumentDto document;
    private int declaredSize;

    /** Restricts creation to the DTO factories. */
    private Builder() {}

    /**
     * Supplies the caseId input.
     *
     * @param value the case identifier to set
     * @return this builder for method chaining
     */
    public Builder caseId(long value) {
      this.caseId = value;
      return this;
    }

    /**
     * Supplies the document input.
     *
     * @param value the document to set
     * @return this builder for method chaining
     */
    public Builder document(DocumentDto value) {
      this.document = value;
      return this;
    }

    /**
     * Supplies the declaredSize input.
     *
     * @param value the declared size to set
     * @return this builder for method chaining
     */
    public Builder declaredSize(int value) {
      this.declaredSize = value;
      return this;
    }

    /**
     * Creates an independent immutable command.
     *
     * @return a new immutable value containing this builder's fields
     */
    public UploadDocumentDto build() {
      return new UploadDocumentDto(this);
    }
  }
}
