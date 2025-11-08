package com.yerin.jobq.service;

import com.yerin.jobq.domain.JobQueuePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnqueueJobService {
    private final JobQueuePort jobQueuePort;

    public String enqueue(String type, String payloadJson, String idempotencyKey) {
        return jobQueuePort.enqueue(type, payloadJson, idempotencyKey);
    }
}