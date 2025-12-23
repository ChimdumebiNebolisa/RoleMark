package com.rolemark.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.entity.Criterion;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CriterionConfigValidator implements ConstraintValidator<ValidCriterionConfig, Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void initialize(ValidCriterionConfig constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        // Basic validation: ensure it's a valid JSON object
        // Detailed type-specific validation happens in service layer where we have access to type
        try {
            JsonNode jsonNode = objectMapper.valueToTree(value);
            return jsonNode.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidConfigForType(Object config, Criterion.CriterionType type) {
        if (config == null) {
            return false;
        }

        JsonNode jsonNode;
        if (config instanceof JsonNode) {
            jsonNode = (JsonNode) config;
        } else {
            try {
                jsonNode = objectMapper.valueToTree(config);
            } catch (Exception e) {
                return false;
            }
        }

        switch (type) {
            case KEYWORD_SKILL:
                return validateKeywordSkillConfig(jsonNode);
            case CUSTOM_KEYWORDS:
                return validateCustomKeywordsConfig(jsonNode);
            case EXPERIENCE_YEARS:
                return validateExperienceYearsConfig(jsonNode);
            case EDUCATION_LEVEL:
                return validateEducationLevelConfig(jsonNode);
            default:
                return false;
        }
    }

    private static boolean validateKeywordSkillConfig(JsonNode jsonNode) {
        if (!jsonNode.has("requiredKeywords") || !jsonNode.get("requiredKeywords").isArray()) {
            return false;
        }
        JsonNode keywords = jsonNode.get("requiredKeywords");
        if (keywords.size() < 1 || keywords.size() > 50) {
            return false;
        }
        for (JsonNode keyword : keywords) {
            if (!keyword.isTextual() || keyword.asText().trim().isEmpty()) {
                return false;
            }
        }
        if (jsonNode.has("matchMode")) {
            String matchMode = jsonNode.get("matchMode").asText();
            if (!"ANY".equals(matchMode) && !"ALL".equals(matchMode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateCustomKeywordsConfig(JsonNode jsonNode) {
        if (!jsonNode.has("keywords") || !jsonNode.get("keywords").isArray()) {
            return false;
        }
        JsonNode keywords = jsonNode.get("keywords");
        if (keywords.size() < 1 || keywords.size() > 50) {
            return false;
        }
        for (JsonNode keyword : keywords) {
            if (!keyword.isTextual() || keyword.asText().trim().isEmpty()) {
                return false;
            }
        }
        if (jsonNode.has("matchMode")) {
            String matchMode = jsonNode.get("matchMode").asText();
            if (!"ANY".equals(matchMode) && !"ALL".equals(matchMode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateExperienceYearsConfig(JsonNode jsonNode) {
        if (!jsonNode.has("requiredYears") || !jsonNode.get("requiredYears").isNumber()) {
            return false;
        }
        int requiredYears = jsonNode.get("requiredYears").asInt();
        if (requiredYears < 0 || requiredYears > 50) {
            return false;
        }
        if (jsonNode.has("targetTitles")) {
            JsonNode targetTitles = jsonNode.get("targetTitles");
            if (!targetTitles.isArray()) {
                return false;
            }
            for (JsonNode title : targetTitles) {
                if (!title.isTextual()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean validateEducationLevelConfig(JsonNode jsonNode) {
        if (!jsonNode.has("minimumLevel") || !jsonNode.get("minimumLevel").isTextual()) {
            return false;
        }
        String level = jsonNode.get("minimumLevel").asText();
        return "HS".equals(level) || "ASSOCIATE".equals(level) ||
               "BACHELOR".equals(level) || "MASTER".equals(level) || "PHD".equals(level);
    }
}

