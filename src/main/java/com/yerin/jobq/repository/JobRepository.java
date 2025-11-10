package com.yerin.jobq.repository;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            JobStatus status, Instant nextAttemptAt);
}
