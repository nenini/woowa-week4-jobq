package com.yerin.jobq.infra;

import com.yerin.jobq.domain.JobQueuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Profile("local-inmem") //local 프로필에 Redis 어대터만 올리기위해서 변경
public class InMemoryQueueAdapter implements JobQueuePort {
    @Override
    public String enqueue(String type, String payloadJson, String idempotencyKey) {
        String jobId = UUID.randomUUID().toString();
        log.info("[InMemoryQueue] enqueue type={}, idKey={}, jobId={}, payload={}",
                type, idempotencyKey, jobId, payloadJson);
        // Day2에 Redis Streams 어댑터로 교체할 예정
        return jobId;
    }

    @Override
    public void enqueueWithJobId(String type, String payloadJson, String idempotencyKey, String jobId) {

    }

    @Override
    public void enqueueDlq(String type, String payloadJson, String jobId) {

    }
}