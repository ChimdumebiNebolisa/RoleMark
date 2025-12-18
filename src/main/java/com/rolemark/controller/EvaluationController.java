package com.rolemark.controller;

import com.rolemark.dto.EvaluationRequest;
import com.rolemark.entity.Evaluation;
import com.rolemark.service.EvaluationService;
import com.rolemark.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    
    private final EvaluationService evaluationService;
    
    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }
    
    @PostMapping
    public ResponseEntity<Evaluation> createEvaluation(@Valid @RequestBody EvaluationRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        Evaluation evaluation = evaluationService.createEvaluation(
                userId, request.getRoleId(), request.getResumeIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluation);
    }
    
    @PostMapping("/{evaluationId}/run")
    public ResponseEntity<Map<String, String>> runEvaluation(@PathVariable Long evaluationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        evaluationService.runEvaluation(userId, evaluationId);
        return ResponseEntity.ok(Map.of("status", "completed", "message", "Evaluation completed successfully"));
    }
    
    @GetMapping
    public ResponseEntity<List<Evaluation>> getAllEvaluations() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Evaluation> evaluations = evaluationService.getAllEvaluations(userId);
        return ResponseEntity.ok(evaluations);
    }
    
    @GetMapping("/{evaluationId}")
    public ResponseEntity<Evaluation> getEvaluationById(@PathVariable Long evaluationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Evaluation evaluation = evaluationService.getEvaluationById(userId, evaluationId);
        return ResponseEntity.ok(evaluation);
    }
    
    @GetMapping("/{evaluationId}/results")
    public ResponseEntity<List<Map<String, Object>>> getEvaluationResults(@PathVariable Long evaluationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Map<String, Object>> results = evaluationService.getEvaluationResults(userId, evaluationId);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{evaluationId}/compare")
    public ResponseEntity<Map<String, Object>> compareResumes(
            @PathVariable Long evaluationId,
            @RequestParam Long leftResumeId,
            @RequestParam Long rightResumeId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> comparison = evaluationService.compareResumes(
                userId, evaluationId, leftResumeId, rightResumeId);
        return ResponseEntity.ok(comparison);
    }
}

