package com.yerin.jobq.repository;

import com.yerin.jobq.domain.JobIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobIdempotencyRepository extends JpaRepository<JobIdempotency, Long> {
    Optional<JobIdempotency> findByIdempotencyKey(String key);
}

