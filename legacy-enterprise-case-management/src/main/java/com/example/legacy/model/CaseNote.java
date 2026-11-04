package com.example.legacy.model;
import java.util.Date;
public class CaseNote {
    private Long id;
    private Long caseId;
    private String author;
    private String body;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long value) { this.caseId = value; }
    public String getAuthor() { return author; }
    public void setAuthor(String value) { this.author = value; }
    public String getBody() { return body; }
    public void setBody(String value) { this.body = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
}
