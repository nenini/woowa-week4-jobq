package com.yerin.jobq.service;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminJobService {
    private final JobRepository jobRepository;
    private final JobQueuePort queue;

    @Transactional
    public Job replay(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("job not found"));

        if (job.getStatus() != JobStatus.DLQ) {
            throw new IllegalStateException("job is not in DLQ");
        }

        // 재시작 : 카운트 초기화 + 즉시 재처리 예약 + 큐 재적재
        job.setStatus(JobStatus.QUEUED);
        job.setRetryCount(0);
        job.setLeaseUntil(null);
        job.setNextAttemptAt(Instant.now());
        job.setQueuedAt(Instant.now());
        jobRepository.save(job);

        queue.enqueueWithJobId(job.getType(), job.getPayloadJson(), null, job.getId().toString());
        return job;
    }
}
