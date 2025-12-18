package com.rolemark.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class EvaluationRequest {
    
    @NotNull(message = "Role ID is required")
    private Long roleId;
    
    @NotEmpty(message = "At least 2 resumes are required")
    @Size(min = 2, max = 10, message = "Evaluation must include 2-10 resumes")
    private List<Long> resumeIds;
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    
    public List<Long> getResumeIds() {
        return resumeIds;
    }
    
    public void setResumeIds(List<Long> resumeIds) {
        this.resumeIds = resumeIds;
    }
}

