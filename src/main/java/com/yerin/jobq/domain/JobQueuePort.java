package com.yerin.jobq.domain;

public interface JobQueuePort {
    String enqueue(String type, String payloadJson, String idempotencyKey);
    // poll/ack/nack 등 추가 예정
}

