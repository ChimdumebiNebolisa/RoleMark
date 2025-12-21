package com.rolemark.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class EvaluationResponse {
    private Long roleId;
    private Long resumeId;
    private Integer totalScore;
    private Map<String, Object> breakdown;
    private LocalDateTime createdAt;
    
    public EvaluationResponse() {
    }
    
    public EvaluationResponse(Long roleId, Long resumeId, Integer totalScore, 
                              Map<String, Object> breakdown, LocalDateTime createdAt) {
        this.roleId = roleId;
        this.resumeId = resumeId;
        this.totalScore = totalScore;
        this.breakdown = breakdown;
        this.createdAt = createdAt;
    }
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    
    public Long getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }
    
    public Integer getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }
    
    public Map<String, Object> getBreakdown() {
        return breakdown;
    }
    
    public void setBreakdown(Map<String, Object> breakdown) {
        this.breakdown = breakdown;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

