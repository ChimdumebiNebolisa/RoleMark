package com.rolemark.service;

import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.entity.Role;
import com.rolemark.exception.ResourceNotFoundException;
import com.rolemark.exception.ValidationException;
import com.rolemark.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public RoleResponse createRole(UUID userId, RoleRequest request) {
        // Check uniqueness constraint
        if (roleRepository.existsByUserIdAndTitle(userId, request.getTitle())) {
            throw new ValidationException("Role title must be unique per user");
        }

        Role role = new Role(userId, request.getTitle(), request.getJobDescription());
        role = roleRepository.save(role);
        return toResponse(role);
    }

    public List<RoleResponse> getAllRoles(UUID userId) {
        return roleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoleResponse getRoleById(UUID userId, UUID roleId) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        return toResponse(role);
    }

    @Transactional
    public RoleResponse updateRole(UUID userId, UUID roleId, RoleRequest request) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Check uniqueness if title is changing
        if (!role.getTitle().equals(request.getTitle()) &&
            roleRepository.existsByUserIdAndTitle(userId, request.getTitle())) {
            throw new ValidationException("Role title must be unique per user");
        }

        role.setTitle(request.getTitle());
        role.setJobDescription(request.getJobDescription());
        role = roleRepository.save(role);
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(UUID userId, UUID roleId) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleRepository.delete(role);
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getTitle(),
                role.getJobDescription(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}

