package com.rolemark.dto;

import com.rolemark.entity.Criterion;

import java.time.Instant;
import java.util.UUID;

public class CriterionResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer weight;
    private Criterion.CriterionType type;
    private Object config;
    private Instant createdAt;
    private Instant updatedAt;

    public CriterionResponse() {
    }

    public CriterionResponse(UUID id, String name, String description, Integer weight,
                            Criterion.CriterionType type, Object config, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.type = type;
        this.config = config;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public Criterion.CriterionType getType() {
        return type;
    }

    public void setType(Criterion.CriterionType type) {
        this.type = type;
    }

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
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

