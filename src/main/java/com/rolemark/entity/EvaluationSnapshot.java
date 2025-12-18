package com.rolemark.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "evaluation_snapshots")
public class EvaluationSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;
    
    @Column(name = "role_title", nullable = false, length = 120)
    private String roleTitle;
    
    @Column(name = "role_job_description", nullable = false, columnDefinition = "TEXT")
    private String roleJobDescription;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "criteria_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> criteriaJson;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getEvaluationId() {
        return evaluationId;
    }
    
    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }
    
    public String getRoleTitle() {
        return roleTitle;
    }
    
    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }
    
    public String getRoleJobDescription() {
        return roleJobDescription;
    }
    
    public void setRoleJobDescription(String roleJobDescription) {
        this.roleJobDescription = roleJobDescription;
    }
    
    public Map<String, Object> getCriteriaJson() {
        return criteriaJson;
    }
    
    public void setCriteriaJson(Map<String, Object> criteriaJson) {
        this.criteriaJson = criteriaJson;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

