package com.example.legacy.model;
import java.util.Date;
public class CaseNotice {
    private Long id;
    private String recipient;
    private String subject;
    private String message;
    private Long caseId;
    private boolean read;
    private Date sentAt;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String value) { this.recipient = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { this.subject = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { this.message = value; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long value) { this.caseId = value; }
    public boolean isRead() { return read; }
    public void setRead(boolean value) { this.read = value; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date value) { this.sentAt = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
}
