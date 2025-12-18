package com.rolemark.repository;

import com.rolemark.entity.ExtractedSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtractedSignalRepository extends JpaRepository<ExtractedSignal, Long> {
    List<ExtractedSignal> findByResumeId(Long resumeId);
    List<ExtractedSignal> findByResumeIdAndType(Long resumeId, String type);
}

