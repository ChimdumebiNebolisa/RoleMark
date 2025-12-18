package com.rolemark.service;

import java.util.*;

public class ExplanationService {
    
    public static String generateExplanation(Map<String, Object> leftBreakdown, Map<String, Object> rightBreakdown) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> leftScores = (List<Map<String, Object>>) leftBreakdown.get("criterionScores");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rightScores = (List<Map<String, Object>>) rightBreakdown.get("criterionScores");
        
        // Calculate deltas for each criterion
        List<CriterionDelta> deltas = new ArrayList<>();
        for (int i = 0; i < leftScores.size(); i++) {
            Map<String, Object> left = leftScores.get(i);
            Map<String, Object> right = rightScores.get(i);
            
            double leftScore = (Double) left.get("score");
            double rightScore = (Double) right.get("score");
            double delta = leftScore - rightScore;
            
            deltas.add(new CriterionDelta(
                    (String) left.get("criterionName"),
                    leftScore,
                    rightScore,
                    delta,
                    (Integer) left.get("weight")
            ));
        }
        
        // Sort by absolute delta (descending)
        deltas.sort((a, b) -> Double.compare(Math.abs(b.delta), Math.abs(a.delta)));
        
        // Build explanation
        StringBuilder explanation = new StringBuilder();
        
        double leftTotal = (Double) leftBreakdown.get("totalScore");
        double rightTotal = (Double) rightBreakdown.get("totalScore");
        
        if (leftTotal > rightTotal) {
            explanation.append("Resume A scored higher due to: ");
        } else if (rightTotal > leftTotal) {
            explanation.append("Resume B scored higher due to: ");
        } else {
            explanation.append("Both resumes scored equally. ");
            return explanation.toString();
        }
        
        // Add top 2 criteria deltas
        List<String> topReasons = new ArrayList<>();
        for (int i = 0; i < Math.min(2, deltas.size()); i++) {
            CriterionDelta delta = deltas.get(i);
            if (Math.abs(delta.delta) > 0.001) { // Only include significant deltas
                String reason = String.format("%s (A: %.2f, B: %.2f, delta: %.2f)",
                        delta.criterionName, delta.leftScore, delta.rightScore, delta.delta);
                topReasons.add(reason);
            }
        }
        
        if (topReasons.isEmpty()) {
            explanation.append("minimal differences across criteria.");
        } else {
            explanation.append(String.join("; ", topReasons));
        }
        
        return explanation.toString();
    }
    
    private static class CriterionDelta {
        String criterionName;
        double leftScore;
        double rightScore;
        double delta;
        int weight;
        
        CriterionDelta(String criterionName, double leftScore, double rightScore, double delta, int weight) {
            this.criterionName = criterionName;
            this.leftScore = leftScore;
            this.rightScore = rightScore;
            this.delta = delta;
            this.weight = weight;
        }
    }
}

