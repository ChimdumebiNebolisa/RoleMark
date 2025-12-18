package com.rolemark.service;

import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.entity.Role;
import com.rolemark.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {
    
    private final RoleRepository roleRepository;
    
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    
    @Transactional
    public RoleResponse createRole(Long userId, RoleRequest request) {
        Role role = new Role();
        role.setUserId(userId);
        role.setTitle(request.getTitle());
        role.setJobDescription(request.getJobDescription());
        role = roleRepository.save(role);
        return toResponse(role);
    }
    
    public List<RoleResponse> getAllRoles(Long userId) {
        return roleRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    public RoleResponse getRoleById(Long userId, Long roleId) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        return toResponse(role);
    }
    
    @Transactional
    public RoleResponse updateRole(Long userId, Long roleId, RoleRequest request) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        role.setTitle(request.getTitle());
        role.setJobDescription(request.getJobDescription());
        role = roleRepository.save(role);
        return toResponse(role);
    }
    
    @Transactional
    public void deleteRole(Long userId, Long roleId) {
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
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

