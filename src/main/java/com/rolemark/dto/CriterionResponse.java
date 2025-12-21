package com.rolemark.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class CriterionResponse {
    private Long id;
    private Long roleId;
    private String name;
    private String description;
    private Integer weight;
    private String type;
    private Map<String, Object> config;
    private List<String> keywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public CriterionResponse() {
    }
    
    // Constructor for Phase 4 Milestone 2 (MVP) - matches spec with Instant
    public CriterionResponse(Long id, Long roleId, String name, int weight, 
                            List<String> keywords, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.roleId = roleId;
        this.name = name;
        this.weight = weight;
        this.keywords = keywords != null ? keywords : List.of();
        this.createdAt = createdAt != null ? 
            LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault()) : null;
        this.updatedAt = updatedAt != null ? 
            LocalDateTime.ofInstant(updatedAt, ZoneId.systemDefault()) : null;
    }
    
    // Legacy constructor for backward compatibility
    public CriterionResponse(Long id, String name, String description, Integer weight, 
                            String type, Map<String, Object> config, 
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.type = type;
        this.config = config;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getWeight() {
        return weight;
    }
    
    public void setWeight(Integer weight) {
        this.weight = weight;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
    
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}

