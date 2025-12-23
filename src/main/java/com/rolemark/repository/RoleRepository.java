package com.rolemark.repository;

import com.rolemark.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Role> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndTitle(UUID userId, String title);
}

