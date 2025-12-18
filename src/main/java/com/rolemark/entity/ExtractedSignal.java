package com.rolemark.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "extracted_signals")
public class ExtractedSignal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    
    @Column(nullable = false, length = 50)
    private String type;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;
    
    @Column(name = "evidence_snippet", length = 300)
    private String evidenceSnippet;
    
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Confidence confidence;
    
    @Column(name = "source_page")
    private Integer sourcePage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public enum Confidence {
        LOW, MEDIUM, HIGH
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getResumeId() {
        return resumeId;
    }
    
    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getEvidenceSnippet() {
        return evidenceSnippet;
    }
    
    public void setEvidenceSnippet(String evidenceSnippet) {
        this.evidenceSnippet = evidenceSnippet;
    }
    
    public Confidence getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }
    
    public Integer getSourcePage() {
        return sourcePage;
    }
    
    public void setSourcePage(Integer sourcePage) {
        this.sourcePage = sourcePage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

