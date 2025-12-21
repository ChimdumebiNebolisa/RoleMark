package com.rolemark.repository;

import com.rolemark.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByUserId(UUID userId);
    Optional<Evaluation> findByIdAndUserId(Long id, UUID userId);
}

