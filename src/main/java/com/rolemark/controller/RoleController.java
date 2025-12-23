package com.rolemark.controller;

import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.security.SecurityUtils;
import com.rolemark.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        RoleResponse response = roleService.createRole(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<RoleResponse> responses = roleService.getAllRoles(userId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID roleId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        RoleResponse response = roleService.getRoleById(userId, roleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable UUID roleId, @Valid @RequestBody RoleRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        RoleResponse response = roleService.updateRole(userId, roleId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        roleService.deleteRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

