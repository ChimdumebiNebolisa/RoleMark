package com.rolemark.repository;

import com.rolemark.entity.EvaluationCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationCandidateRepository extends JpaRepository<EvaluationCandidate, Long> {
    List<EvaluationCandidate> findByEvaluationId(Long evaluationId);
}

