package com.rolemark.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;

public class CriterionConfigValidator implements ConstraintValidator<ValidCriterionConfig, Map<String, Object>> {
    
    @Override
    public void initialize(ValidCriterionConfig constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(Map<String, Object> config, ConstraintValidatorContext context) {
        if (config == null) {
            return false;
        }
        
        // This will be called after type is validated, so we validate based on type
        // The actual type-specific validation is done in CriterionService
        return true;
    }
    
    public static void validateConfigByType(String type, Map<String, Object> config) {
        switch (type) {
            case "KEYWORD_SKILL":
                validateKeywordSkillConfig(config);
                break;
            case "CUSTOM_KEYWORDS":
                validateCustomKeywordsConfig(config);
                break;
            case "EXPERIENCE_YEARS":
                validateExperienceYearsConfig(config);
                break;
            case "EDUCATION_LEVEL":
                validateEducationLevelConfig(config);
                break;
            default:
                throw new IllegalArgumentException("Invalid criterion type: " + type);
        }
    }
    
    private static void validateKeywordSkillConfig(Map<String, Object> config) {
        if (!config.containsKey("requiredKeywords")) {
            throw new IllegalArgumentException("requiredKeywords is required for KEYWORD_SKILL");
        }
        Object keywordsObj = config.get("requiredKeywords");
        if (!(keywordsObj instanceof List)) {
            throw new IllegalArgumentException("requiredKeywords must be an array");
        }
        List<?> keywords = (List<?>) keywordsObj;
        if (keywords.isEmpty() || keywords.size() > 50) {
            throw new IllegalArgumentException("requiredKeywords must have 1-50 items");
        }
        for (Object keyword : keywords) {
            if (!(keyword instanceof String)) {
                throw new IllegalArgumentException("All keywords must be strings");
            }
        }
        String matchMode = (String) config.getOrDefault("matchMode", "ANY");
        if (!matchMode.equals("ANY") && !matchMode.equals("ALL")) {
            throw new IllegalArgumentException("matchMode must be 'ANY' or 'ALL'");
        }
    }
    
    private static void validateCustomKeywordsConfig(Map<String, Object> config) {
        if (!config.containsKey("keywords")) {
            throw new IllegalArgumentException("keywords is required for CUSTOM_KEYWORDS");
        }
        Object keywordsObj = config.get("keywords");
        if (!(keywordsObj instanceof List)) {
            throw new IllegalArgumentException("keywords must be an array");
        }
        List<?> keywords = (List<?>) keywordsObj;
        if (keywords.isEmpty() || keywords.size() > 50) {
            throw new IllegalArgumentException("keywords must have 1-50 items");
        }
        for (Object keyword : keywords) {
            if (!(keyword instanceof String)) {
                throw new IllegalArgumentException("All keywords must be strings");
            }
        }
        String matchMode = (String) config.getOrDefault("matchMode", "ANY");
        if (!matchMode.equals("ANY") && !matchMode.equals("ALL")) {
            throw new IllegalArgumentException("matchMode must be 'ANY' or 'ALL'");
        }
    }
    
    private static void validateExperienceYearsConfig(Map<String, Object> config) {
        if (!config.containsKey("requiredYears")) {
            throw new IllegalArgumentException("requiredYears is required for EXPERIENCE_YEARS");
        }
        Object yearsObj = config.get("requiredYears");
        if (!(yearsObj instanceof Number)) {
            throw new IllegalArgumentException("requiredYears must be a number");
        }
        double years = ((Number) yearsObj).doubleValue();
        if (years < 0 || years > 50) {
            throw new IllegalArgumentException("requiredYears must be between 0 and 50");
        }
        // targetTitles is optional
        if (config.containsKey("targetTitles")) {
            Object titlesObj = config.get("targetTitles");
            if (!(titlesObj instanceof List)) {
                throw new IllegalArgumentException("targetTitles must be an array");
            }
        }
    }
    
    private static void validateEducationLevelConfig(Map<String, Object> config) {
        if (!config.containsKey("minimumLevel")) {
            throw new IllegalArgumentException("minimumLevel is required for EDUCATION_LEVEL");
        }
        String level = (String) config.get("minimumLevel");
        List<String> validLevels = List.of("HS", "ASSOCIATE", "BACHELOR", "MASTER", "PHD");
        if (!validLevels.contains(level)) {
            throw new IllegalArgumentException("minimumLevel must be one of: " + validLevels);
        }
    }
}

