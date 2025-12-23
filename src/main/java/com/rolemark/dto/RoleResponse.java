package com.rolemark.dto;

import java.time.Instant;
import java.util.UUID;

public class RoleResponse {
    private UUID id;
    private String title;
    private String jobDescription;
    private Instant createdAt;
    private Instant updatedAt;

    public RoleResponse() {
    }

    public RoleResponse(UUID id, String title, String jobDescription, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.jobDescription = jobDescription;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

