package com.yerin.jobq.infra;

import com.yerin.jobq.domain.JobQueuePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class RedisStreamsQueueAdapter implements JobQueuePort {
    private final StringRedisTemplate redis;

    @Value("${jobq.stream.prefix:jobq:stream}")
    private String streamPrefix;

    @Value("${jobq.stream.group:jobq:cg}")
    private String groupName;

    @Override
    public void enqueueWithJobId(String type, String payloadJson, String idempotencyKey, String jobId) {
        String streamKey = streamPrefix + ":" + type;
        ensureGroup(streamKey, groupName);

        Map<String, String> fields = new HashMap<>();
        fields.put("type", type);
        fields.put("payload", payloadJson);
        if (idempotencyKey != null) fields.put("idempotencyKey", idempotencyKey);
        fields.put("jobId", jobId);
        fields.put("enqueuedAt", Instant.now().toString());

        RecordId rid = redis.opsForStream().add(StreamRecords.mapBacked(fields).withStreamKey(streamKey));
        log.info("[RedisStream] XADD key={}, id={}, jobId={}, type={}", streamKey, rid, jobId, type);
    }

    @Override
    public String enqueue(String type, String payloadJson, String idempotencyKey) {
        String streamKey = streamPrefix + ":" + type;

        ensureGroup(streamKey, groupName);

        Map<String, String> fields = new HashMap<>();
        fields.put("type", type);
        fields.put("payload", payloadJson);
        if (idempotencyKey != null) fields.put("idempotencyKey", idempotencyKey);
        fields.put("enqueuedAt", Instant.now().toString());

        String jobId = UUID.randomUUID().toString();
        fields.put("jobId", jobId);

        RecordId rid = redis.opsForStream().add(StreamRecords.mapBacked(fields).withStreamKey(streamKey));
        log.info("[RedisStream] XADD key={}, id={}, jobId={}, type={}", streamKey, rid, jobId, type);

        return jobId;
    }

    private void ensureGroup(String streamKey, String group) {
        try {
            redis.opsForStream().createGroup(streamKey, ReadOffset.latest(), group);
            log.info("[RedisStream] createGroup key={}, group={}", streamKey, group);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("BUSYGROUP") || msg.contains("already exists")) {
            } else {
                log.warn("[RedisStream] createGroup ignored key={}, group={}, cause={}", streamKey, group, msg);
            }
        }
    }
}
