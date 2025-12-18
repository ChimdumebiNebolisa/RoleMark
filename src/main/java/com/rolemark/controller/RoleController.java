package com.rolemark.controller;

import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.service.RoleService;
import com.rolemark.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    
    private final RoleService roleService;
    
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }
    
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        RoleResponse response = roleService.createRole(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<RoleResponse> roles = roleService.getAllRoles(userId);
        return ResponseEntity.ok(roles);
    }
    
    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long roleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.getRoleById(userId, roleId);
        return ResponseEntity.ok(role);
    }
    
    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.updateRole(userId, roleId, request);
        return ResponseEntity.ok(role);
    }
    
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long roleId) {
        Long userId = SecurityUtil.getCurrentUserId();
        roleService.deleteRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}

