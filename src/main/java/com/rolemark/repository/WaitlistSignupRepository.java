package com.rolemark.repository;

import com.rolemark.entity.WaitlistSignup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaitlistSignupRepository extends JpaRepository<WaitlistSignup, Long> {
}

