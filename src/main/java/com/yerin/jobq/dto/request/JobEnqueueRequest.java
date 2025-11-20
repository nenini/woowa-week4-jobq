package com.yerin.jobq.dto.request;

public record JobEnqueueRequest(
        Long userId,
        String idempotencyKey
) {}
