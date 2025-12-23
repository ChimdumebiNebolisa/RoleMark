package com.rolemark.controller;

import com.rolemark.dto.CriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.security.SecurityUtils;
import com.rolemark.service.CriterionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles/{roleId}/criteria")
public class CriterionController {

    private final CriterionService criterionService;

    public CriterionController(CriterionService criterionService) {
        this.criterionService = criterionService;
    }

    @PostMapping
    public ResponseEntity<CriterionResponse> createCriterion(@PathVariable Long roleId, @Valid @RequestBody CriterionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CriterionResponse response = criterionService.createCriterion(userId, roleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CriterionResponse>> getAllCriteria(@PathVariable Long roleId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CriterionResponse> responses = criterionService.getAllCriteria(userId, roleId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{criterionId}")
    public ResponseEntity<CriterionResponse> getCriterionById(@PathVariable Long roleId, @PathVariable Long criterionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        CriterionResponse response = criterionService.getCriterionById(userId, roleId, criterionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{criterionId}")
    public ResponseEntity<CriterionResponse> updateCriterion(@PathVariable Long roleId, @PathVariable Long criterionId, @Valid @RequestBody CriterionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CriterionResponse response = criterionService.updateCriterion(userId, roleId, criterionId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{criterionId}")
    public ResponseEntity<Void> deleteCriterion(@PathVariable Long roleId, @PathVariable Long criterionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        criterionService.deleteCriterion(userId, roleId, criterionId);
        return ResponseEntity.noContent().build();
    }
}

