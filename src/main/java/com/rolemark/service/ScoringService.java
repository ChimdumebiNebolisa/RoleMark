package com.rolemark.service;

import com.rolemark.entity.*;
import com.rolemark.repository.ExtractedSignalRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringService {
    
    private final ExtractedSignalRepository extractedSignalRepository;
    
    public ScoringService(ExtractedSignalRepository extractedSignalRepository) {
        this.extractedSignalRepository = extractedSignalRepository;
    }
    
    public Map<String, Object> scoreResume(Resume resume, List<Criterion> criteria, String resumeText) {
        Map<String, Object> breakdown = new HashMap<>();
        List<Map<String, Object>> criterionScores = new ArrayList<>();
        double totalWeightedScore = 0.0;
        
        for (Criterion criterion : criteria) {
            Map<String, Object> criterionScore = scoreCriterion(criterion, resume, resumeText);
            criterionScores.add(criterionScore);
            
            double score = (Double) criterionScore.get("score");
            int weight = criterion.getWeight();
            double weightedScore = score * (weight / 100.0);
            totalWeightedScore += weightedScore;
        }
        
        // Normalize total score to [0.0, 1.0]
        double totalScore = Math.max(0.0, Math.min(1.0, totalWeightedScore));
        double totalScorePct = totalScore * 100.0;
        
        breakdown.put("criterionScores", criterionScores);
        breakdown.put("totalScore", totalScore);
        breakdown.put("totalScorePct", Math.round(totalScorePct * 10.0) / 10.0);
        
        return breakdown;
    }
    
    private Map<String, Object> scoreCriterion(Criterion criterion, Resume resume, String resumeText) {
        Map<String, Object> result = new HashMap<>();
        result.put("criterionId", criterion.getId());
        result.put("criterionName", criterion.getName());
        result.put("weight", criterion.getWeight());
        result.put("type", criterion.getType());
        
        double score = 0.0;
        List<String> evidence = new ArrayList<>();
        
        switch (criterion.getType()) {
            case "KEYWORD_SKILL":
                score = scoreKeywordSkill(criterion, resumeText, evidence);
                break;
            case "CUSTOM_KEYWORDS":
                score = scoreCustomKeywords(criterion, resumeText, evidence);
                break;
            case "EXPERIENCE_YEARS":
                score = scoreExperienceYears(criterion, resume, evidence);
                break;
            case "EDUCATION_LEVEL":
                score = scoreEducationLevel(criterion, resume, evidence);
                break;
            default:
                throw new IllegalArgumentException("Unknown criterion type: " + criterion.getType());
        }
        
        result.put("score", score);
        result.put("evidence", evidence);
        
        return result;
    }
    
    private double scoreKeywordSkill(Criterion criterion, String resumeText, List<String> evidence) {
        Map<String, Object> config = criterion.getConfigJson();
        @SuppressWarnings("unchecked")
        List<String> requiredKeywords = (List<String>) config.get("requiredKeywords");
        String matchMode = (String) config.getOrDefault("matchMode", "ANY");
        
        ResumeParserService parserService = new ResumeParserService();
        String normalizedText = parserService.normalizeText(resumeText);
        
        int matchedCount = 0;
        List<String> matchedKeywords = new ArrayList<>();
        
        for (String keyword : requiredKeywords) {
            String normalizedKeyword = parserService.normalizeText(keyword);
            if (normalizedText.contains(normalizedKeyword)) {
                matchedCount++;
                matchedKeywords.add(keyword);
            }
        }
        
        // Calculate raw score
        double rawScore = (double) matchedCount / requiredKeywords.size();
        
        // Clamp to [0, 1]
        double score = Math.max(0.0, Math.min(1.0, rawScore));
        
        // Store evidence (first 3 matched keywords)
        for (int i = 0; i < Math.min(3, matchedKeywords.size()); i++) {
            String keyword = matchedKeywords.get(i);
            int index = normalizedText.indexOf(parserService.normalizeText(keyword));
            if (index >= 0) {
                String snippet = extractSnippet(resumeText, index, keyword.length());
                evidence.add("Matched keyword '" + keyword + "': " + snippet);
            }
        }
        
        return score;
    }
    
    private double scoreCustomKeywords(Criterion criterion, String resumeText, List<String> evidence) {
        // Same logic as KEYWORD_SKILL
        return scoreKeywordSkill(criterion, resumeText, evidence);
    }
    
    private double scoreExperienceYears(Criterion criterion, Resume resume, List<String> evidence) {
        Map<String, Object> config = criterion.getConfigJson();
        double requiredYears = ((Number) config.get("requiredYears")).doubleValue();
        
        if (requiredYears == 0) {
            return 1.0;
        }
        
        // Get experience years estimate from extracted signals
        List<ExtractedSignal> signals = extractedSignalRepository.findByResumeIdAndType(
                resume.getId(), "EXPERIENCE_YEARS_ESTIMATE");
        
        double candidateYears = 0.0;
        if (!signals.isEmpty()) {
            try {
                candidateYears = Double.parseDouble(signals.get(0).getValue());
            } catch (NumberFormatException e) {
                candidateYears = 0.0;
            }
            
            // Add evidence snippets
            List<ExtractedSignal> dateRangeSignals = extractedSignalRepository.findByResumeIdAndType(
                    resume.getId(), "DATE_RANGE");
            for (int i = 0; i < Math.min(3, dateRangeSignals.size()); i++) {
                evidence.add(dateRangeSignals.get(i).getEvidenceSnippet());
            }
        }
        
        // Score = min(candidateYears / requiredYears, 1.0)
        double score = Math.min(candidateYears / requiredYears, 1.0);
        
        if (evidence.isEmpty()) {
            evidence.add("No date ranges detected in resume");
        }
        
        return score;
    }
    
    private double scoreEducationLevel(Criterion criterion, Resume resume, List<String> evidence) {
        Map<String, Object> config = criterion.getConfigJson();
        String minimumLevel = (String) config.get("minimumLevel");
        
        // Map levels to numeric values
        Map<String, Double> levelValues = new HashMap<>();
        levelValues.put("UNKNOWN", 0.0);
        levelValues.put("HS", 0.25);
        levelValues.put("ASSOCIATE", 0.45);
        levelValues.put("BACHELOR", 0.65);
        levelValues.put("MASTER", 0.85);
        levelValues.put("PHD", 1.0);
        
        double minRequiredValue = levelValues.get(minimumLevel);
        
        // Get education level from extracted signals
        List<ExtractedSignal> signals = extractedSignalRepository.findByResumeIdAndType(
                resume.getId(), "EDUCATION_LEVEL_ESTIMATE");
        
        String candidateLevel = "UNKNOWN";
        if (!signals.isEmpty()) {
            candidateLevel = signals.get(0).getValue();
            String snippet = signals.get(0).getEvidenceSnippet();
            if (snippet != null && !snippet.isEmpty()) {
                evidence.add(snippet);
            }
        }
        
        double candidateLevelValue = levelValues.getOrDefault(candidateLevel, 0.0);
        
        // Calculate score
        double score;
        if (candidateLevelValue >= minRequiredValue) {
            score = 1.0;
        } else {
            score = candidateLevelValue / minRequiredValue; // Partial credit
        }
        
        if (evidence.isEmpty()) {
            evidence.add("No education token detected");
        }
        
        return score;
    }
    
    private String extractSnippet(String text, int startIndex, int length) {
        int snippetStart = Math.max(0, startIndex - 40);
        int snippetEnd = Math.min(text.length(), startIndex + length + 40);
        return text.substring(snippetStart, snippetEnd).trim();
    }
}

