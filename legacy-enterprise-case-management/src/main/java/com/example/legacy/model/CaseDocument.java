package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped attachment containing file metadata and the document bytes stored in the
 * database.
 *
 * @author Chuck Emmons
 */
public class CaseDocument {
  private Long id;
  private Long caseId;
  private String originalName;
  private String contentType;
  private int contentSize;
  private byte[] payload;
  private String uploadedBy;
  private Date createdAt;

  /**
   * Returns database-generated identifier.
   *
   * @return database-generated identifier
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets database-generated identifier.
   *
   * @param value the record identifier to set
   */
  public void setId(Long value) {
    this.id = value;
  }

  /**
   * Returns associated case identifier.
   *
   * @return associated case identifier
   */
  public Long getCaseId() {
    return caseId;
  }

  /**
   * Sets associated case identifier.
   *
   * @param value the case identifier to set
   */
  public void setCaseId(Long value) {
    this.caseId = value;
  }

  /**
   * Returns stored attachment filename.
   *
   * @return stored attachment filename
   */
  public String getOriginalName() {
    return originalName;
  }

  /**
   * Sets stored attachment filename.
   *
   * @param value the original name to set
   */
  public void setOriginalName(String value) {
    this.originalName = value;
  }

  /**
   * Returns attachment media type.
   *
   * @return attachment media type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets attachment media type.
   *
   * @param value the MIME content type to set
   */
  public void setContentType(String value) {
    this.contentType = value;
  }

  /**
   * Returns attachment size in bytes.
   *
   * @return attachment size in bytes
   */
  public int getContentSize() {
    return contentSize;
  }

  /**
   * Sets attachment size in bytes.
   *
   * @param value the content size to set
   */
  public void setContentSize(int value) {
    this.contentSize = value;
  }

  /**
   * Returns attachment byte array without making a defensive copy.
   *
   * @return attachment byte array without making a defensive copy
   */
  public byte[] getPayload() {
    return payload;
  }

  /**
   * Sets attachment byte array without making a defensive copy.
   *
   * @param value the payload to set
   */
  public void setPayload(byte[] value) {
    this.payload = value;
  }

  /**
   * Returns uploader username.
   *
   * @return uploader username
   */
  public String getUploadedBy() {
    return uploadedBy;
  }

  /**
   * Sets uploader username.
   *
   * @param value the uploaded by to set
   */
  public void setUploadedBy(String value) {
    this.uploadedBy = value;
  }

  /**
   * Returns creation timestamp.
   *
   * @return creation timestamp
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets creation timestamp.
   *
   * @param value the creation time to set
   */
  public void setCreatedAt(Date value) {
    this.createdAt = value;
  }
}
