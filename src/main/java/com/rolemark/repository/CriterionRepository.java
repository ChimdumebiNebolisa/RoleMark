package com.rolemark.repository;

import com.rolemark.entity.Criterion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, Long> {
    List<Criterion> findByRoleIdOrderByCreatedAtAsc(Long roleId);
    Optional<Criterion> findByIdAndRoleId(Long id, Long roleId);
    int countByRoleId(Long roleId);
}

