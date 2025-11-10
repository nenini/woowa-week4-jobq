package com.yerin.jobq.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class WorkerRunner {

    private final StringRedisTemplate redis;

    @Value("${jobq.stream.prefix:jobq:stream}")
    private String streamPrefix;

    @Value("${jobq.stream.group:jobq:cg}")
    private String groupName;

    @Value("${jobq.worker.batchSize:10}")
    private long batchSize;

    @Value("${jobq.worker.blockMillis:2000}")
    private long blockMillis;

    private final String consumerName = WorkerId.consumerName();

    private String streamKey(String type) {
        return streamPrefix + ":" + type;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        String key = streamKey("email_welcome");

        try {
            ensureGroup(key, groupName);

            List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                    Consumer.from(groupName, consumerName),
                    StreamReadOptions.empty()
                            .count(batchSize)
                            .block(Duration.ofMillis(blockMillis)),
                    StreamOffset.create(key, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, Object, Object> rec : records) {
                try {
                    handleRecord(key, rec);
                    redis.opsForStream().acknowledge(key, groupName, rec.getId());
                    log.info("[Worker] ACK key={}, id={}", key, rec.getId());
                } catch (Exception ex) {
                    log.error("[Worker] handle error id={}, cause={}", rec.getId(), ex.toString());
                }
            }
        } catch (Exception e) {
            log.warn("[Worker] poll error: {}", e.toString());
        }
    }

    private void handleRecord(String key, MapRecord<String, Object, Object> rec) {
        // 간단히 콘솔 처리(해피패스)
        String jobId = (String) rec.getValue().get("jobId");
        String type  = (String) rec.getValue().get("type");
        String payload = (String) rec.getValue().get("payload");

        log.info("[Worker] handle key={}, id={}, jobId={}, type={}, payload={}",
                key, rec.getId(), jobId, type, payload);

        // TODO: DB 상태 전이 QUEUED -> RUNNING -> SUCCEEDED
        // - 핸들러 디스패쳐 구조(타입별 핸들러 매핑)
        // - 실패 시 재시도/지수백오프
    }

    private void ensureGroup(String streamKey, String group) {
        try {
            redis.opsForStream().createGroup(streamKey, ReadOffset.latest(), group);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("BUSYGROUP") || msg.contains("already exists")) {
                // OK
            }
        }
    }
}

