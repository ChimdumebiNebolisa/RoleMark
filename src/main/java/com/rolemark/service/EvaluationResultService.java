package com.rolemark.service;

import com.rolemark.entity.Criterion;
import com.rolemark.entity.EvaluationResult;
import com.rolemark.entity.Resume;
import com.rolemark.entity.Role;
import com.rolemark.exception.AccessDeniedException;
import com.rolemark.exception.NotFoundException;
import com.rolemark.repository.CriterionRepository;
import com.rolemark.repository.EvaluationResultRepository;
import com.rolemark.repository.ResumeRepository;
import com.rolemark.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing evaluation results (Phase 4 Milestone 3).
 * Implements deterministic evaluation engine.
 */
@Service
public class EvaluationResultService {
    
    private final EvaluationResultRepository evaluationResultRepository;
    private final RoleRepository roleRepository;
    private final ResumeRepository resumeRepository;
    private final CriterionRepository criterionRepository;
    
    public EvaluationResultService(EvaluationResultRepository evaluationResultRepository,
                                   RoleRepository roleRepository,
                                   ResumeRepository resumeRepository,
                                   CriterionRepository criterionRepository) {
        this.evaluationResultRepository = evaluationResultRepository;
        this.roleRepository = roleRepository;
        this.resumeRepository = resumeRepository;
        this.criterionRepository = criterionRepository;
    }
    
    /**
     * Evaluate a resume against a role and persist the result.
     * Implements deterministic scoring: keyword matching with weight-based points.
     */
    @Transactional
    public EvaluationResult evaluate(UUID userId, Long roleId, Long resumeId) {
        // Validate role ownership - return 403 if exists but belongs to another user, 404 if truly missing
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));
        if (!role.getUserId().equals(userId)) {
            throw new AccessDeniedException("Role does not belong to current user");
        }
        
        // Validate resume ownership - return 403 if exists but belongs to another user, 404 if truly missing
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        if (!resume.getUserId().equals(userId)) {
            throw new AccessDeniedException("Resume does not belong to current user");
        }
        
        // Fetch criteria for (userId, roleId)
        List<Criterion> criteria = criterionRepository.findByUserIdAndRoleId(userId, roleId);
        
        // Normalize resume text: lowercase
        String normalizedText = resume.getRawExtractedText().toLowerCase();
        
        // Compute deterministic score
        Map<String, Object> breakdown = new HashMap<>();
        int totalScore = 0;
        
        for (Criterion criterion : criteria) {
            // Check keyword matches (substring match on normalized text)
            List<String> matchedKeywords = new ArrayList<>();
            for (String keyword : criterion.getKeywords()) {
                String normalizedKeyword = keyword.toLowerCase();
                if (normalizedText.contains(normalizedKeyword)) {
                    matchedKeywords.add(keyword); // Store original keyword case
                }
            }
            
            // points = weight if at least 1 keyword matched else 0
            int points = matchedKeywords.isEmpty() ? 0 : criterion.getWeight();
            totalScore += points;
            
            // Build breakdown entry: { "points": <int>, "matched": ["java","spring"] }
            Map<String, Object> criterionBreakdown = new HashMap<>();
            criterionBreakdown.put("points", points);
            criterionBreakdown.put("matched", matchedKeywords);
            breakdown.put(criterion.getId().toString(), criterionBreakdown);
        }
        
        // Clamp totalScore to 0..100
        totalScore = Math.max(0, Math.min(100, totalScore));
        
        // UPSERT: Delete existing if present, then insert new
        evaluationResultRepository.findByUserIdAndRoleIdAndResumeId(userId, roleId, resumeId)
                .ifPresent(existing -> evaluationResultRepository.delete(existing));
        
        // Persist result
        EvaluationResult result = new EvaluationResult();
        result.setUserId(userId);
        result.setRoleId(roleId);
        result.setResumeId(resumeId);
        result.setTotalScore(totalScore);
        result.setBreakdown(breakdown);
        result = evaluationResultRepository.save(result);
        
        return result;
    }
    
    /**
     * Compare evaluation results for two resumes.
     * TODO: Implement comparison logic in later milestone.
     */
    public Map<String, Object> compare(UUID userId, Long roleId, Long leftResumeId, Long rightResumeId) {
        // TODO: Implement comparison logic
        // This is a skeleton that compiles but does not implement comparison
        throw new UnsupportedOperationException("Comparison logic not yet implemented");
    }
    
    /**
     * Get all evaluation results for a role, ordered by total score descending.
     * Validates role ownership before returning results.
     */
    public List<EvaluationResult> getResultsByRole(UUID userId, Long roleId) {
        // Validate role ownership - return 403 if exists but belongs to another user, 404 if truly missing
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found"));
        if (!role.getUserId().equals(userId)) {
            throw new AccessDeniedException("Role does not belong to current user");
        }
        
        return evaluationResultRepository.findByUserIdAndRoleIdOrderByTotalScoreDesc(userId, roleId);
    }
    
    /**
     * Get evaluation result for a specific resume in a role.
     */
    public EvaluationResult getResultByResume(UUID userId, Long roleId, Long resumeId) {
        return evaluationResultRepository.findByUserIdAndRoleIdAndResumeId(userId, roleId, resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation result not found"));
    }
    
    /**
     * Check if an evaluation result exists for a resume in a role.
     */
    public boolean existsResult(UUID userId, Long roleId, Long resumeId) {
        return evaluationResultRepository.existsByUserIdAndRoleIdAndResumeId(userId, roleId, resumeId);
    }
}

