package com.rolemark.repository;

import com.rolemark.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, UUID> {
    List<Criterion> findByRoleIdOrderByCreatedAtAsc(UUID roleId);
    Optional<Criterion> findByIdAndRoleId(UUID id, UUID roleId);
    int countByRoleId(UUID roleId);
}

