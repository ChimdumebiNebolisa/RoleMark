package com.rolemark.controller;

import com.rolemark.dto.CreateCriterionRequest;
import com.rolemark.dto.CriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.service.CriterionService;
import com.rolemark.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CriterionController {
    
    private final CriterionService criterionService;
    
    public CriterionController(CriterionService criterionService) {
        this.criterionService = criterionService;
    }
    
    // Phase 4 Milestone 2: MVP endpoints
    
    @PostMapping("/roles/{roleId}/criteria")
    public ResponseEntity<CriterionResponse> createCriterion(
            @PathVariable Long roleId, 
            @Valid @RequestBody CreateCriterionRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse response = criterionService.createCriterion(userId, roleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/roles/{roleId}/criteria")
    public ResponseEntity<List<CriterionResponse>> getCriteriaForRole(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<CriterionResponse> criteria = criterionService.getCriteriaForRole(userId, roleId);
        return ResponseEntity.ok(criteria);
    }
    
    @DeleteMapping("/criteria/{id}")
    public ResponseEntity<Void> deleteCriterion(@PathVariable Long id) {
        UUID userId = SecurityUtil.getCurrentUserId();
        criterionService.deleteCriterionById(userId, id);
        return ResponseEntity.noContent().build();
    }
    
    // Legacy endpoints (kept for backward compatibility)
    
    @PostMapping("/roles/{roleId}/criteria/legacy")
    public ResponseEntity<CriterionResponse> createCriterionLegacy(
            @PathVariable Long roleId, 
            @Valid @RequestBody CriterionRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse response = criterionService.createCriterion(userId, roleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/roles/{roleId}/criteria/legacy")
    public ResponseEntity<List<CriterionResponse>> getAllCriteria(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<CriterionResponse> criteria = criterionService.getAllCriteria(userId, roleId);
        return ResponseEntity.ok(criteria);
    }
    
    @GetMapping("/roles/{roleId}/criteria/{criterionId}")
    public ResponseEntity<CriterionResponse> getCriterionById(
            @PathVariable Long roleId, 
            @PathVariable Long criterionId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse criterion = criterionService.getCriterionById(userId, roleId, criterionId);
        return ResponseEntity.ok(criterion);
    }
    
    @PutMapping("/roles/{roleId}/criteria/{criterionId}")
    public ResponseEntity<CriterionResponse> updateCriterion(
            @PathVariable Long roleId, 
            @PathVariable Long criterionId, 
            @Valid @RequestBody CriterionRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse criterion = criterionService.updateCriterion(userId, roleId, criterionId, request);
        return ResponseEntity.ok(criterion);
    }
    
    @DeleteMapping("/roles/{roleId}/criteria/{criterionId}")
    public ResponseEntity<Void> deleteCriterionLegacy(
            @PathVariable Long roleId, 
            @PathVariable Long criterionId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        criterionService.deleteCriterion(userId, roleId, criterionId);
        return ResponseEntity.noContent().build();
    }
}

