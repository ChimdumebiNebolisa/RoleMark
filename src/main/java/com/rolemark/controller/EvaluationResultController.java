package com.rolemark.controller;

import com.rolemark.dto.CompareResponse;
import com.rolemark.dto.EvaluationResponse;
import com.rolemark.entity.EvaluationResult;
import com.rolemark.service.EvaluationResultService;
import com.rolemark.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class EvaluationResultController {
    
    private final EvaluationResultService evaluationResultService;
    
    public EvaluationResultController(EvaluationResultService evaluationResultService) {
        this.evaluationResultService = evaluationResultService;
    }
    
    /**
     * Evaluate a resume against a role's criteria.
     * POST /api/roles/{roleId}/evaluate/{resumeId}
     */
    @PostMapping("/{roleId}/evaluate/{resumeId}")
    public ResponseEntity<EvaluationResponse> evaluate(
            @PathVariable Long roleId,
            @PathVariable Long resumeId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        EvaluationResult result = evaluationResultService.evaluate(userId, roleId, resumeId);
        
        EvaluationResponse response = new EvaluationResponse(
                result.getRoleId(),
                result.getResumeId(),
                result.getTotalScore(),
                result.getBreakdown(),
                result.getCreatedAt()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Compare evaluation results for a role, ordered by total score descending.
     * GET /api/roles/{roleId}/compare
     */
    @GetMapping("/{roleId}/compare")
    public ResponseEntity<CompareResponse> compare(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<EvaluationResult> results = evaluationResultService.getResultsByRole(userId, roleId);
        
        List<EvaluationResponse> responseList = results.stream()
                .map(result -> new EvaluationResponse(
                        result.getRoleId(),
                        result.getResumeId(),
                        result.getTotalScore(),
                        result.getBreakdown(),
                        result.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(new CompareResponse(responseList));
    }
}

