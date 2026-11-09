package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped discussion note associated with a case and its author.
 *
 * @author Chuck Emmons
 */
public class CaseNote {
  private Long id;
  private Long caseId;
  private String author;
  private String body;
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
   * Returns note author username.
   *
   * @return note author username
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Sets note author username.
   *
   * @param value the author to set
   */
  public void setAuthor(String value) {
    this.author = value;
  }

  /**
   * Returns note text.
   *
   * @return note text
   */
  public String getBody() {
    return body;
  }

  /**
   * Sets note text.
   *
   * @param value the body to set
   */
  public void setBody(String value) {
    this.body = value;
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
