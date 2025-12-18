package com.rolemark.repository;

import com.rolemark.entity.ScoreBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreBreakdownRepository extends JpaRepository<ScoreBreakdown, Long> {
    List<ScoreBreakdown> findByEvaluationId(Long evaluationId);
    Optional<ScoreBreakdown> findByEvaluationIdAndResumeId(Long evaluationId, Long resumeId);
}

