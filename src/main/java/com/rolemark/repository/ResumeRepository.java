package com.rolemark.repository;

import com.rolemark.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserId(UUID userId);
    Optional<Resume> findByIdAndUserId(Long id, UUID userId);
    Optional<Resume> findByChecksumSha256(String checksum);
}

