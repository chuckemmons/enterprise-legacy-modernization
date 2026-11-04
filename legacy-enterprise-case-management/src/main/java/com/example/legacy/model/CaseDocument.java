package com.example.legacy.model;
import java.util.Date;
public class CaseDocument {
    private Long id;
    private Long caseId;
    private String originalName;
    private String contentType;
    private int contentSize;
    private byte[] payload;
    private String uploadedBy;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long value) { this.caseId = value; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String value) { this.originalName = value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { this.contentType = value; }
    public int getContentSize() { return contentSize; }
    public void setContentSize(int value) { this.contentSize = value; }
    public byte[] getPayload() { return payload; }
    public void setPayload(byte[] value) { this.payload = value; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String value) { this.uploadedBy = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
}
