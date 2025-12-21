package com.rolemark.repository;

import com.rolemark.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByUserId(UUID userId);
    Optional<Role> findByIdAndUserId(Long id, UUID userId);
    boolean existsByUserIdAndTitle(UUID userId, String title);
}

