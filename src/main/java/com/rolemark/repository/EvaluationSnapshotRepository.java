package com.rolemark.repository;

import com.rolemark.entity.EvaluationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationSnapshotRepository extends JpaRepository<EvaluationSnapshot, Long> {
    Optional<EvaluationSnapshot> findByEvaluationId(Long evaluationId);
}

