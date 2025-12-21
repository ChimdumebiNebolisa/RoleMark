package com.rolemark.service;

import com.rolemark.entity.EvaluationResult;
import com.rolemark.repository.EvaluationResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing evaluation results (Phase 4 Milestone 1).
 * Minimal skeleton - implementations are TODO but must compile.
 */
@Service
public class EvaluationResultService {
    
    private final EvaluationResultRepository evaluationResultRepository;
    
    public EvaluationResultService(EvaluationResultRepository evaluationResultRepository) {
        this.evaluationResultRepository = evaluationResultRepository;
    }
    
    /**
     * Evaluate a resume against a role and persist the result.
     * TODO: Implement scoring logic in later milestone.
     */
    @Transactional
    public EvaluationResult evaluate(UUID userId, Long roleId, Long resumeId) {
        // TODO: Implement evaluation logic
        // This is a skeleton that compiles but does not implement scoring
        throw new UnsupportedOperationException("Evaluation logic not yet implemented");
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
     */
    public List<EvaluationResult> getResultsByRole(UUID userId, Long roleId) {
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

