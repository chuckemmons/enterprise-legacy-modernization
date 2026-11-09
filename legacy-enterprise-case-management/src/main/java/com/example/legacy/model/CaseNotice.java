package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped recipient notification with independent read and email-delivery timestamps.
 *
 * @author Chuck Emmons
 */
public class CaseNotice {
  private Long id;
  private String recipient;
  private String subject;
  private String message;
  private Long caseId;
  private boolean read;
  private Date sentAt;
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
   * Returns notification recipient username.
   *
   * @return notification recipient username
   */
  public String getRecipient() {
    return recipient;
  }

  /**
   * Sets notification recipient username.
   *
   * @param value the recipient to set
   */
  public void setRecipient(String value) {
    this.recipient = value;
  }

  /**
   * Returns notification subject.
   *
   * @return notification subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets notification subject.
   *
   * @param value the subject to set
   */
  public void setSubject(String value) {
    this.subject = value;
  }

  /**
   * Returns notification body.
   *
   * @return notification body
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets notification body.
   *
   * @param value the message to set
   */
  public void setMessage(String value) {
    this.message = value;
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
   * Returns whether the recipient has read the notification.
   *
   * @return whether the recipient has read the notification
   */
  public boolean isRead() {
    return read;
  }

  /**
   * Sets whether the recipient has read the notification.
   *
   * @param value the read to set
   */
  public void setRead(boolean value) {
    this.read = value;
  }

  /**
   * Returns email-processing timestamp, or {@code null} while pending.
   *
   * @return email-processing timestamp, or {@code null} while pending
   */
  public Date getSentAt() {
    return sentAt;
  }

  /**
   * Sets email-processing timestamp, or {@code null} while pending.
   *
   * @param value the sent at to set
   */
  public void setSentAt(Date value) {
    this.sentAt = value;
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
