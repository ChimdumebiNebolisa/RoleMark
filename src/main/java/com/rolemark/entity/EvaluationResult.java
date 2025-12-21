package com.rolemark.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing deterministic evaluation results (scoring output).
 * 
 * <p><strong>Naming Convention:</strong>
 * <ul>
 *   <li><code>evaluations</code> table: Evaluation run metadata/status (pipeline job tracking with CREATED/RUNNING/COMPLETED/FAILED status)</li>
 *   <li><code>evaluation_results</code> table: Deterministic scoring output (total_score + breakdown JSONB per resume)</li>
 * </ul>
 * These serve different purposes: evaluations tracks the pipeline execution, evaluation_results stores computed scores.
 * 
 * <p>Maps to the <code>evaluation_results</code> table created in V4 migration.
 */
@Entity
@Table(name = "evaluation_results")
public class EvaluationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;
    
    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> breakdown;
    
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
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
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

