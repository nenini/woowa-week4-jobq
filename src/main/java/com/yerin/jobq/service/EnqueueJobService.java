package com.yerin.jobq.service;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobIdempotency;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobIdempotencyRepository;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EnqueueJobService {
    private final JobQueuePort jobQueuePort;
    private final JobRepository jobRepository;
    private final JobIdempotencyRepository idemRepo;


//    public String enqueue(String type, String payloadJson, String idempotencyKey) {
//        return jobQueuePort.enqueue(type, payloadJson, idempotencyKey);
//    }

    @Transactional
    public String enqueue(String type, String payloadJson, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var hit = idemRepo.findByIdempotencyKey(idempotencyKey);
            if (hit.isPresent()) return hit.get().getJobId().toString();
        }

        Job job = Job.builder()
                .type(type)
                .payloadJson(payloadJson)
                .status(JobStatus.QUEUED)
                .retryCount(0)
                .nextAttemptAt(Instant.now())
                .build();
        job = jobRepository.save(job);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                idemRepo.save(JobIdempotency.builder()
                        .idempotencyKey(idempotencyKey)
                        .jobId(job.getId())
                        .createdAt(Instant.now())
                        .build());
            } catch (DataIntegrityViolationException e) {
                Long keep = idemRepo.findByIdempotencyKey(idempotencyKey)
                        .map(JobIdempotency::getJobId)
                        .orElse(job.getId());
                return keep.toString();
            }
        }

        job.setQueuedAt(Instant.now());
        jobRepository.save(job);

        jobQueuePort.enqueueWithJobId(type, payloadJson, idempotencyKey, job.getId().toString());
        return job.getId().toString();
    }
}