package com.rolemark.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "score_breakdowns")
public class ScoreBreakdown {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;
    
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    
    @Column(name = "total_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal totalScore;
    
    @Column(name = "total_score_pct", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalScorePct;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> breakdownJson;
    
    @Column(name = "explanation_text", nullable = false, columnDefinition = "TEXT")
    private String explanationText;
    
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
    
    public Long getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }
    
    public BigDecimal getTotalScore() {
        return totalScore;
    }
    
    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }
    
    public BigDecimal getTotalScorePct() {
        return totalScorePct;
    }
    
    public void setTotalScorePct(BigDecimal totalScorePct) {
        this.totalScorePct = totalScorePct;
    }
    
    public Map<String, Object> getBreakdownJson() {
        return breakdownJson;
    }
    
    public void setBreakdownJson(Map<String, Object> breakdownJson) {
        this.breakdownJson = breakdownJson;
    }
    
    public String getExplanationText() {
        return explanationText;
    }
    
    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

