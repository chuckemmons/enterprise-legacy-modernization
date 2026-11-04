package com.example.legacy.model;
import java.util.Date;
public class Account {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String email;
    private String role;
    private String department;
    private boolean enabled;
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { this.passwordHash = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email = value; }
    public String getRole() { return role; }
    public void setRole(String value) { this.role = value; }
    public String getDepartment() { return department; }
    public void setDepartment(String value) { this.department = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { this.enabled = value; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date value) { this.createdAt = value; }
}
