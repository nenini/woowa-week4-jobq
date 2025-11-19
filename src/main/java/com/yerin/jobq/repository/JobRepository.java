package com.yerin.jobq.repository;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            JobStatus status, Instant nextAttemptAt);
    List<Job> findTop100ByStatusAndLeaseUntilLessThanEqualOrderByLeaseUntilAsc(
            JobStatus status, Instant leaseUntil);
    long countByStatus(JobStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update Job j
          set j.status = :status,
              j.leaseUntil = :leaseUntil
        where j.id = :id
          and j.status = :expectedCurrent
       """)
    int updateStatusAndLeaseIf(@Param("id") Long id,
                               @Param("expectedCurrent") JobStatus expectedCurrent,
                               @Param("status") JobStatus nextStatus,
                               @Param("leaseUntil") Instant leaseUntil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update Job j
          set j.status = :status,
              j.retryCount = :retry,
              j.nextAttemptAt = :nextAttemptAt,
              j.leaseUntil = null
        where j.id = :id
          and j.status = :expectedCurrent
       """)
    int updateRetryIf(@Param("id") Long id,
                      @Param("expectedCurrent") JobStatus expectedCurrent,
                      @Param("status") JobStatus nextStatus,
                      @Param("retry") int retry,
                      @Param("nextAttemptAt") Instant nextAttemptAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
   update Job j
      set j.status = com.yerin.jobq.domain.JobStatus.SUCCEEDED,
          j.leaseUntil = null
    where j.id = :id
      and j.status = com.yerin.jobq.domain.JobStatus.RUNNING
   """)
    int succeedIfRunning(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
   update Job j
      set j.status = com.yerin.jobq.domain.JobStatus.DLQ,
          j.leaseUntil = null
    where j.id = :id
      and j.status = com.yerin.jobq.domain.JobStatus.RUNNING
   """)
    int dlqIfRunning(@Param("id") Long id);


}
