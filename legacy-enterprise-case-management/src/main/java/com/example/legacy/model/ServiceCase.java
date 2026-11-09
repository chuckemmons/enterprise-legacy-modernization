package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped case record containing workflow state, ownership, dates, and the optimistic-lock
 * version.
 *
 * @author Chuck Emmons
 */
public class ServiceCase {
  private Long id;
  private String caseNumber;
  private String title;
  private String description;
  private String category;
  private String priority;
  private String status;
  private String createdBy;
  private String assignedTo;
  private String department;
  private Date dueAt;
  private Date closedAt;
  private Date createdAt;
  private Date updatedAt;
  private Integer version;
  private boolean archived;
  private boolean overdueNoticeSent;

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
   * Returns human-readable case number.
   *
   * @return human-readable case number
   */
  public String getCaseNumber() {
    return caseNumber;
  }

  /**
   * Sets human-readable case number.
   *
   * @param value the case number to set
   */
  public void setCaseNumber(String value) {
    this.caseNumber = value;
  }

  /**
   * Returns case title.
   *
   * @return case title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets case title.
   *
   * @param value the title to set
   */
  public void setTitle(String value) {
    this.title = value;
  }

  /**
   * Returns case description.
   *
   * @return case description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets case description.
   *
   * @param value the description to set
   */
  public void setDescription(String value) {
    this.description = value;
  }

  /**
   * Returns case category.
   *
   * @return case category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Sets case category.
   *
   * @param value the category to set
   */
  public void setCategory(String value) {
    this.category = value;
  }

  /**
   * Returns case priority.
   *
   * @return case priority
   */
  public String getPriority() {
    return priority;
  }

  /**
   * Sets case priority.
   *
   * @param value the priority to set
   */
  public void setPriority(String value) {
    this.priority = value;
  }

  /**
   * Returns workflow status.
   *
   * @return workflow status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets workflow status.
   *
   * @param value the status to set
   */
  public void setStatus(String value) {
    this.status = value;
  }

  /**
   * Returns case creator username.
   *
   * @return case creator username
   */
  public String getCreatedBy() {
    return createdBy;
  }

  /**
   * Sets case creator username.
   *
   * @param value the creator's username to set
   */
  public void setCreatedBy(String value) {
    this.createdBy = value;
  }

  /**
   * Returns assignee username, or {@code null} when unassigned.
   *
   * @return assignee username, or {@code null} when unassigned
   */
  public String getAssignedTo() {
    return assignedTo;
  }

  /**
   * Sets assignee username, or {@code null} when unassigned.
   *
   * @param value the assignee's username to set
   */
  public void setAssignedTo(String value) {
    this.assignedTo = value;
  }

  /**
   * Returns department name.
   *
   * @return department name
   */
  public String getDepartment() {
    return department;
  }

  /**
   * Sets department name.
   *
   * @param value the department to set
   */
  public void setDepartment(String value) {
    this.department = value;
  }

  /**
   * Returns due timestamp, or {@code null} when no due date is set.
   *
   * @return due timestamp, or {@code null} when no due date is set
   */
  public Date getDueAt() {
    return dueAt;
  }

  /**
   * Sets due timestamp, or {@code null} when no due date is set.
   *
   * @param value the due date to set
   */
  public void setDueAt(Date value) {
    this.dueAt = value;
  }

  /**
   * Returns closure timestamp, or {@code null} before closure.
   *
   * @return closure timestamp, or {@code null} before closure
   */
  public Date getClosedAt() {
    return closedAt;
  }

  /**
   * Sets closure timestamp, or {@code null} before closure.
   *
   * @param value the closure time to set
   */
  public void setClosedAt(Date value) {
    this.closedAt = value;
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

  /**
   * Returns last-update timestamp.
   *
   * @return last-update timestamp
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets last-update timestamp.
   *
   * @param value the last update time to set
   */
  public void setUpdatedAt(Date value) {
    this.updatedAt = value;
  }

  /**
   * Returns Hibernate optimistic-lock version.
   *
   * @return Hibernate optimistic-lock version
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * Sets Hibernate optimistic-lock version.
   *
   * @param value the optimistic-lock version to set
   */
  public void setVersion(Integer value) {
    this.version = value;
  }

  /**
   * Returns whether the case is soft-deleted.
   *
   * @return whether the case is soft-deleted
   */
  public boolean isArchived() {
    return archived;
  }

  /**
   * Sets whether the case is soft-deleted.
   *
   * @param value the archived flag to set
   */
  public void setArchived(boolean value) {
    this.archived = value;
  }

  /**
   * Returns whether an overdue notification has already been recorded.
   *
   * @return whether an overdue notification has already been recorded
   */
  public boolean isOverdueNoticeSent() {
    return overdueNoticeSent;
  }

  /**
   * Sets whether an overdue notification has already been recorded.
   *
   * @param value the overdue-notification flag to set
   */
  public void setOverdueNoticeSent(boolean value) {
    this.overdueNoticeSent = value;
  }
}
