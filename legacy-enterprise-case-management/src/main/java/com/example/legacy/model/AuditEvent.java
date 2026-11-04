package com.example.legacy.model;
import java.util.Date;
public class AuditEvent {
    private Long id;
    private String entityType;
    private Long entityId;
    private String actionName;
    private String actor;
    private String details;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { this.entityType = value; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long value) { this.entityId = value; }
    public String getActionName() { return actionName; }
    public void setActionName(String value) { this.actionName = value; }
    public String getActor() { return actor; }
    public void setActor(String value) { this.actor = value; }
    public String getDetails() { return details; }
    public void setDetails(String value) { this.details = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
}
