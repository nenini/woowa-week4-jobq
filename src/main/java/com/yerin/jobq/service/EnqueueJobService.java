package com.yerin.jobq.service;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnqueueJobService {
    private final JobQueuePort jobQueuePort;
    private final JobRepository jobRepository;

//    public String enqueue(String type, String payloadJson, String idempotencyKey) {
//        return jobQueuePort.enqueue(type, payloadJson, idempotencyKey);
//    }

    @Transactional
    public String enqueue(String type, String payloadJson, String idempotencyKey) {
        Job job = Job.builder()
                .type(type)
                .payloadJson(payloadJson)
                .status(JobStatus.QUEUED)
                .retryCount(0)
                .build();
        job = jobRepository.save(job);

        String jobId = job.getId().toString();
        jobQueuePort.enqueueWithJobId(type, payloadJson, idempotencyKey, jobId);
        return jobId;
    }
}