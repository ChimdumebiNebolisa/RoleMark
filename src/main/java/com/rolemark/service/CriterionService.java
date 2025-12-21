package com.rolemark.service;

import com.rolemark.dto.CreateCriterionRequest;
import com.rolemark.dto.CriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.entity.Criterion;
import com.rolemark.entity.Role;
import com.rolemark.exception.AccessDeniedException;
import com.rolemark.exception.DuplicateResourceException;
import com.rolemark.exception.NotFoundException;
import com.rolemark.repository.CriterionRepository;
import com.rolemark.repository.RoleRepository;
import com.rolemark.validator.CriterionConfigValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    
    // Phase 4 Milestone 2: MVP methods with CreateCriterionRequest
    
    @Transactional
    public CriterionResponse createCriterion(UUID userId, Long roleId, CreateCriterionRequest request) {
        // Verify role ownership - throw 403 if not owned by user
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Role does not belong to current user"));
        
        // Validate name (non-blank, max 120)
        String name = request.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("Name must not exceed 120 characters");
        }
        
        // Validate weight (0..100)
        Integer weight = request.getWeight();
        if (weight == null || weight < 0 || weight > 100) {
            throw new IllegalArgumentException("Weight must be between 0 and 100");
        }
        
        // Validate keywords (non-null, default to empty list)
        List<String> keywords = request.getKeywords();
        if (keywords == null) {
            keywords = java.util.Collections.emptyList();
        }
        
        // Check for duplicate name (userId, roleId, name) -> 409 if duplicate
        if (criterionRepository.existsByUserIdAndRoleIdAndName(userId, roleId, name.trim())) {
            throw new DuplicateResourceException("Criterion with name '" + name + "' already exists for this role");
        }
        
        // Create criterion
        Criterion criterion = new Criterion();
        criterion.setUserId(userId);
        criterion.setRoleId(roleId);
        criterion.setName(name.trim());
        criterion.setWeight(weight);
        criterion.setKeywords(keywords);
        // Set defaults for fields not in MVP
        criterion.setDescription(null);
        criterion.setType("KEYWORD"); // Default type for MVP
        criterion.setConfigJson(java.util.Map.of()); // Empty config for MVP
        criterion = criterionRepository.save(criterion);
        
        return toMvpResponse(criterion);
    }
    
    public List<CriterionResponse> getCriteriaForRole(UUID userId, Long roleId) {
        // Verify role ownership - throw 403 if not owned by user
        roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new AccessDeniedException("Role does not belong to current user"));
        
        // Return only criteria for this user and role
        return criterionRepository.findByUserIdAndRoleId(userId, roleId).stream()
                .map(this::toMvpResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteCriterionById(UUID userId, Long criterionId) {
        // Verify criterion ownership - throw 403 if not owned by user, 404 if not found
        Criterion criterion = criterionRepository.findByIdAndUserId(criterionId, userId)
                .orElseThrow(() -> new NotFoundException("Criterion not found"));
        
        criterionRepository.delete(criterion);
    }
    
    // Helper to convert Criterion to MVP CriterionResponse
    private CriterionResponse toMvpResponse(Criterion criterion) {
        LocalDateTime createdAt = criterion.getCreatedAt();
        LocalDateTime updatedAt = criterion.getUpdatedAt();
        Instant createdAtInstant = createdAt != null ? 
            createdAt.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant updatedAtInstant = updatedAt != null ? 
            updatedAt.atZone(ZoneId.systemDefault()).toInstant() : null;
        
        return new CriterionResponse(
                criterion.getId(),
                criterion.getRoleId(),
                criterion.getName(),
                criterion.getWeight(),
                criterion.getKeywords() != null ? criterion.getKeywords() : java.util.Collections.emptyList(),
                createdAtInstant,
                updatedAtInstant
        );
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

