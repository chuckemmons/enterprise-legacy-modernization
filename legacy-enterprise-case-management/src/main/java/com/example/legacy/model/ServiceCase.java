package com.example.legacy.model;
import java.util.Date;
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

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String value) { this.caseNumber = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category = value; }
    public String getPriority() { return priority; }
    public void setPriority(String value) { this.priority = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { this.createdBy = value; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String value) { this.assignedTo = value; }
    public String getDepartment() { return department; }
    public void setDepartment(String value) { this.department = value; }
    public Date getDueAt() { return dueAt; }
    public void setDueAt(Date value) { this.dueAt = value; }
    public Date getClosedAt() { return closedAt; }
    public void setClosedAt(Date value) { this.closedAt = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date value) { this.updatedAt = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version = value; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean value) { this.archived = value; }
    public boolean isOverdueNoticeSent() { return overdueNoticeSent; }
    public void setOverdueNoticeSent(boolean value) { this.overdueNoticeSent = value; }
}
