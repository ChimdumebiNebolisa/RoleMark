package com.rolemark.controller;

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
@RequestMapping("/api/roles/{roleId}/criteria")
public class CriterionController {
    
    private final CriterionService criterionService;
    
    public CriterionController(CriterionService criterionService) {
        this.criterionService = criterionService;
    }
    
    @PostMapping
    public ResponseEntity<CriterionResponse> createCriterion(@PathVariable Long roleId, @Valid @RequestBody CriterionRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse response = criterionService.createCriterion(userId, roleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<CriterionResponse>> getAllCriteria(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<CriterionResponse> criteria = criterionService.getAllCriteria(userId, roleId);
        return ResponseEntity.ok(criteria);
    }
    
    @GetMapping("/{criterionId}")
    public ResponseEntity<CriterionResponse> getCriterionById(@PathVariable Long roleId, @PathVariable Long criterionId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse criterion = criterionService.getCriterionById(userId, roleId, criterionId);
        return ResponseEntity.ok(criterion);
    }
    
    @PutMapping("/{criterionId}")
    public ResponseEntity<CriterionResponse> updateCriterion(@PathVariable Long roleId, @PathVariable Long criterionId, @Valid @RequestBody CriterionRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        CriterionResponse criterion = criterionService.updateCriterion(userId, roleId, criterionId, request);
        return ResponseEntity.ok(criterion);
    }
    
    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Void> deleteCriterion(@PathVariable Long roleId, @PathVariable Long criterionId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        criterionService.deleteCriterion(userId, roleId, criterionId);
        return ResponseEntity.noContent().build();
    }
}

