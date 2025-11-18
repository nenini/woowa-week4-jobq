package com.yerin.jobq.repository;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            JobStatus status, Instant nextAttemptAt);
    List<Job> findTop100ByStatusAndLeaseUntilLessThanEqualOrderByLeaseUntilAsc(
            JobStatus status, Instant leaseUntil);
    long countByStatus(JobStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Job j set j.status = :status, j.leaseUntil = :leaseUntil where j.id = :id")
    int updateStatusAndLease(@Param("id") Long id,
                             @Param("status") JobStatus status,
                             @Param("leaseUntil") Instant leaseUntil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Job j set j.status = :status, j.retryCount = :retry, j.nextAttemptAt = :nextAttemptAt, j.leaseUntil = null where j.id = :id")
    int updateRetry(@Param("id") Long id,
                    @Param("status") JobStatus status,
                    @Param("retry") int retry,
                    @Param("nextAttemptAt") Instant nextAttemptAt);

}
