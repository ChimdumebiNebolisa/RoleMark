package com.rolemark.dto;

import com.rolemark.entity.Criterion;
import com.rolemark.validation.ValidCriterionConfig;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CriterionRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name must not exceed 80 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be between 0 and 100")
    @Max(value = 100, message = "Weight must be between 0 and 100")
    private Integer weight;

    @NotNull(message = "Type is required")
    private Criterion.CriterionType type;

    @NotNull(message = "Config is required")
    @ValidCriterionConfig
    private Object config;

    public CriterionRequest() {
    }

    public CriterionRequest(String name, String description, Integer weight, Criterion.CriterionType type, Object config) {
        this.name = name;
        this.description = description;
        this.weight = weight;
        this.type = type;
        this.config = config;
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
}

