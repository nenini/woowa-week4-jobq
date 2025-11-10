package com.yerin.jobq.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        Instant updatedAt
) {}
