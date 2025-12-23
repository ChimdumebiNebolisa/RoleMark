package com.rolemark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.dto.CriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.entity.Criterion;
import com.rolemark.exception.ResourceNotFoundException;
import com.rolemark.repository.CriterionRepository;
import com.rolemark.validation.CriterionConfigValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CriterionService {

    private final CriterionRepository criterionRepository;
    private final RoleService roleService;
    private final ObjectMapper objectMapper;

    private static final int MAX_CRITERIA_PER_ROLE = 15;

    public CriterionService(CriterionRepository criterionRepository,
                           RoleService roleService,
                           ObjectMapper objectMapper) {
        this.criterionRepository = criterionRepository;
        this.roleService = roleService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CriterionResponse createCriterion(Long userId, Long roleId, CriterionRequest request) {
        // Verify role ownership
        roleService.getRoleById(userId, roleId);

        // Check max criteria limit
        int currentCount = criterionRepository.countByRoleId(roleId);
        if (currentCount >= MAX_CRITERIA_PER_ROLE) {
            throw new IllegalArgumentException("Maximum " + MAX_CRITERIA_PER_ROLE + " criteria allowed per role");
        }

        // Validate config based on type
        if (!CriterionConfigValidator.isValidConfigForType(request.getConfig(), request.getType())) {
            throw new IllegalArgumentException("Invalid config for criterion type: " + request.getType());
        }

        // Serialize config to JSON
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(request.getConfig());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config format", e);
        }

        Criterion criterion = new Criterion(
                roleId,
                request.getName(),
                request.getDescription(),
                request.getWeight(),
                request.getType(),
                configJson
        );

        criterion = criterionRepository.save(criterion);
        return toResponse(criterion);
    }

    public List<CriterionResponse> getAllCriteria(Long userId, Long roleId) {
        // Verify role ownership
        roleService.getRoleById(userId, roleId);

        return criterionRepository.findByRoleIdOrderByCreatedAtAsc(roleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CriterionResponse getCriterionById(Long userId, Long roleId, Long criterionId) {
        // Verify role ownership
        roleService.getRoleById(userId, roleId);

        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
        return toResponse(criterion);
    }

    @Transactional
    public CriterionResponse updateCriterion(Long userId, Long roleId, Long criterionId, CriterionRequest request) {
        // Verify role ownership
        roleService.getRoleById(userId, roleId);

        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));

        // Validate config based on type
        if (!CriterionConfigValidator.isValidConfigForType(request.getConfig(), request.getType())) {
            throw new IllegalArgumentException("Invalid config for criterion type: " + request.getType());
        }

        // Serialize config to JSON
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(request.getConfig());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid config format", e);
        }

        criterion.setName(request.getName());
        criterion.setDescription(request.getDescription());
        criterion.setWeight(request.getWeight());
        criterion.setType(request.getType());
        criterion.setConfigJson(configJson);

        criterion = criterionRepository.save(criterion);
        return toResponse(criterion);
    }

    @Transactional
    public void deleteCriterion(Long userId, Long roleId, Long criterionId) {
        // Verify role ownership
        roleService.getRoleById(userId, roleId);

        Criterion criterion = criterionRepository.findByIdAndRoleId(criterionId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Criterion not found"));
        criterionRepository.delete(criterion);
    }

    private CriterionResponse toResponse(Criterion criterion) {
        Object config;
        try {
            config = objectMapper.readValue(criterion.getConfigJson(), Object.class);
        } catch (Exception e) {
            config = criterion.getConfigJson(); // Fallback to string if parsing fails
        }

        return new CriterionResponse(
                criterion.getId(),
                criterion.getName(),
                criterion.getDescription(),
                criterion.getWeight(),
                criterion.getType(),
                config,
                criterion.getCreatedAt(),
                criterion.getUpdatedAt()
        );
    }
}

