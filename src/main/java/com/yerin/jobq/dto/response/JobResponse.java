package com.yerin.jobq.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobResponse(
        Long id,
        String type,
        JobStatus status,
        Integer retryCount,
        Instant nextAttemptAt,
        Instant leaseUntil,
        Instant createdAt,
        Instant updatedAt,
        Instant queuedAt
) {
    public static JobResponse from(Job j) {
        return new JobResponse(
                j.getId(),
                j.getType(),
                j.getStatus(),
                j.getRetryCount(),
                j.getNextAttemptAt(),
                j.getLeaseUntil(),
                j.getCreatedAt(),
                j.getUpdatedAt(),
                j.getQueuedAt()
        );
    }
}
