package com.rolemark.repository;

import com.rolemark.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, Long> {
    List<Criterion> findByRoleId(Long roleId);
    Optional<Criterion> findByIdAndRoleId(Long id, Long roleId);
    
    // Phase 4 Milestone 1: User-scoped methods
    List<Criterion> findByUserIdAndRoleId(UUID userId, Long roleId);
    boolean existsByUserIdAndRoleIdAndName(UUID userId, Long roleId, String name);
    Optional<Criterion> findByIdAndUserId(Long id, UUID userId);
    
    @Query("SELECT SUM(c.weight) FROM Criterion c WHERE c.roleId = :roleId")
    Integer sumWeightsByRoleId(@Param("roleId") Long roleId);
    
    @Query("SELECT COUNT(c) FROM Criterion c WHERE c.roleId = :roleId")
    Long countByRoleId(@Param("roleId") Long roleId);
}

