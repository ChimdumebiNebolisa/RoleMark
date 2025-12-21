package com.rolemark.repository;

import com.rolemark.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {
    // Phase 4 Milestone 1: Required methods
    List<EvaluationResult> findByUserIdAndRoleIdOrderByTotalScoreDesc(UUID userId, Long roleId);
    Optional<EvaluationResult> findByUserIdAndRoleIdAndResumeId(UUID userId, Long roleId, Long resumeId);
    boolean existsByUserIdAndRoleIdAndResumeId(UUID userId, Long roleId, Long resumeId);
}

