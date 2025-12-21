package com.rolemark.service;

import com.rolemark.dto.CriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.entity.Criterion;
import com.rolemark.entity.Role;
import com.rolemark.repository.CriterionRepository;
import com.rolemark.repository.RoleRepository;
import com.rolemark.validator.CriterionConfigValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CriterionService {
    
    private final CriterionRepository criterionRepository;
    private final RoleRepository roleRepository;
    
    public CriterionService(CriterionRepository criterionRepository, RoleRepository roleRepository) {
        this.criterionRepository = criterionRepository;
        this.roleRepository = roleRepository;
    }
    
    @Transactional
    public CriterionResponse createCriterion(UUID userId, Long roleId, CriterionRequest request) {
        // Verify role ownership
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        // Check criteria count limit (max 15)
        Long currentCount = criterionRepository.countByRoleId(roleId);
        if (currentCount >= 15) {
            throw new IllegalArgumentException("Maximum 15 criteria allowed per role");
        }
        
        // Validate config based on type
        CriterionConfigValidator.validateConfigByType(request.getType(), request.getConfig());
        
        Criterion criterion = new Criterion();
        criterion.setUserId(userId);
        criterion.setRoleId(roleId);
        criterion.setName(request.getName());
        criterion.setDescription(request.getDescription());
        criterion.setWeight(request.getWeight());
        criterion.setType(request.getType());
        criterion.setConfigJson(request.getConfig());
        // TODO: Set keywords from request when available
        criterion.setKeywords(java.util.Collections.emptyList());
        criterion = criterionRepository.save(criterion);
        
        return toResponse(criterion);
    }
    
    public List<CriterionResponse> getAllCriteria(UUID userId, Long roleId) {
        // Verify role ownership
        roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        // Use user-scoped method for Phase 4
        return criterionRepository.findByUserIdAndRoleId(userId, roleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public CriterionResponse getCriterionById(UUID userId, Long roleId, Long criterionId) {
        // Verify role ownership
        roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("Criterion not found"));
        return toResponse(criterion);
    }
    
    @Transactional
    public CriterionResponse updateCriterion(UUID userId, Long roleId, Long criterionId, CriterionRequest request) {
        // Verify role ownership
        roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("Criterion not found"));
        
        // Validate config based on type
        CriterionConfigValidator.validateConfigByType(request.getType(), request.getConfig());
        
        criterion.setName(request.getName());
        criterion.setDescription(request.getDescription());
        criterion.setWeight(request.getWeight());
        criterion.setType(request.getType());
        criterion.setConfigJson(request.getConfig());
        criterion = criterionRepository.save(criterion);
        
        return toResponse(criterion);
    }
    
    @Transactional
    public void deleteCriterion(UUID userId, Long roleId, Long criterionId) {
        // Verify role ownership
        roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new IllegalArgumentException("Criterion not found"));
        criterionRepository.delete(criterion);
    }
    
    public void validateWeightsSumTo100(Long roleId) {
        Integer sum = criterionRepository.sumWeightsByRoleId(roleId);
        if (sum == null || sum != 100) {
            throw new IllegalArgumentException("Criteria weights must sum to exactly 100. Current sum: " + (sum == null ? 0 : sum));
        }
    }
    
    private CriterionResponse toResponse(Criterion criterion) {
        return new CriterionResponse(
                criterion.getId(),
                criterion.getName(),
                criterion.getDescription(),
                criterion.getWeight(),
                criterion.getType(),
                criterion.getConfigJson(),
                criterion.getCreatedAt(),
                criterion.getUpdatedAt()
        );
    }
}

