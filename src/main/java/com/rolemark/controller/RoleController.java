package com.rolemark.controller;

import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.service.RoleService;
import com.rolemark.util.SecurityUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    
    private static final Logger log = LoggerFactory.getLogger(RoleController.class);
    private final RoleService roleService;
    
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("Creating role for user {} with title: {}", userId, request.getTitle());
        RoleResponse response = roleService.createRole(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<RoleResponse> roles = roleService.getAllRoles(userId);
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.getRoleById(userId, roleId);
        return ResponseEntity.ok(role);
    }
    
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        UUID userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.updateRole(userId, roleId, request);
        return ResponseEntity.ok(role);
    }
    
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        roleService.deleteRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

