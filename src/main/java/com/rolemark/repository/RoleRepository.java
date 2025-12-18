package com.rolemark.repository;

import com.rolemark.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    List<Role> findByUserId(Long userId);
    Optional<Role> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndTitle(Long userId, String title);
}

