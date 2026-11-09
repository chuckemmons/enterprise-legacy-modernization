package com.example.legacy.model;

import java.util.Date;

/**
 * Hibernate-mapped record of an entity mutation, its actor, details, and timestamp.
 *
 * @author Chuck Emmons
 */
public class AuditEvent {
  private Long id;
  private String entityType;
  private Long entityId;
  private String actionName;
  private String actor;
  private String details;
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
   * Returns audited entity type.
   *
   * @return audited entity type
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Sets audited entity type.
   *
   * @param value the entity type to set
   */
  public void setEntityType(String value) {
    this.entityType = value;
  }

  /**
   * Returns audited entity identifier.
   *
   * @return audited entity identifier
   */
  public Long getEntityId() {
    return entityId;
  }

  /**
   * Sets audited entity identifier.
   *
   * @param value the entity ID to set
   */
  public void setEntityId(Long value) {
    this.entityId = value;
  }

  /**
   * Returns audited action name.
   *
   * @return audited action name
   */
  public String getActionName() {
    return actionName;
  }

  /**
   * Sets audited action name.
   *
   * @param value the action name to set
   */
  public void setActionName(String value) {
    this.actionName = value;
  }

  /**
   * Returns username responsible for the audit event.
   *
   * @return username responsible for the audit event
   */
  public String getActor() {
    return actor;
  }

  /**
   * Sets username responsible for the audit event.
   *
   * @param value the actor to set
   */
  public void setActor(String value) {
    this.actor = value;
  }

  /**
   * Returns audit detail text.
   *
   * @return audit detail text
   */
  public String getDetails() {
    return details;
  }

  /**
   * Sets audit detail text.
   *
   * @param value the details to set
   */
  public void setDetails(String value) {
    this.details = value;
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
