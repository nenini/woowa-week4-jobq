package com.yerin.jobq.infra;

import com.yerin.jobq.application.JobHandler;
import com.yerin.jobq.application.JobHandlerRegistry;
import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobEventLog;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobEventLogRepository;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local")
public class WorkerRunner {

    private final StringRedisTemplate redis;
    private final JobRepository jobRepository;
    private final JobEventLogRepository logRepository;
    private final JobHandlerRegistry registry;
    private final JobQueuePort jobQueuePort;

    @Value("${jobq.stream.prefix:jobq:stream}")
    private String streamPrefix;

    @Value("${jobq.stream.group:jobq:cg}")
    private String groupName;

    @Value("${jobq.worker.batchSize:10}")
    private long batchSize;

    @Value("${jobq.worker.blockMillis:2000}")
    private long blockMillis;

    @Value("${jobq.retry.maxRetries:5}")
    private int maxRetries;

    @Value("${jobq.retry.baseBackoffMillis:1000}")
    private long baseBackoffMillis;

    @Value("${jobq.retry.backoffCapMillis:60000}")
    private long backoffCapMillis;

    @Value("${jobq.retry.jitterRatio:0.2}")
    private double jitterRatio;

    @Value("${jobq.leaseSeconds:30}")
    private long leaseSeconds;

    private final String consumerName = WorkerId.consumerName();

    private String streamKey(String type) {
        return streamPrefix + ":" + type;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        String type = "email_welcome"; // Day3: 데모 1종
        String key = streamKey(type);
        ensureGroup(key, groupName);

        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(groupName, consumerName),
                StreamReadOptions.empty().count(batchSize).block(Duration.ofMillis(blockMillis)),
                StreamOffset.create(key, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> rec : records) {
            String jobIdStr = (String) rec.getValue().get("jobId");
            String payload  = (String) rec.getValue().get("payload");

            try {
                Long jobId = Long.valueOf(jobIdStr);
                Job job = jobRepository.findById(jobId).orElse(null);
                if (job == null) { ack(key, rec); continue; }

                // 아직 시간이 안 됐으면 처리하지 않고 스킵(ACK)
                if (job.getNextAttemptAt() != null && job.getNextAttemptAt().isAfter(Instant.now())) {
                    ack(key, rec);
                    log.info("[Worker] skip(not-due) key={}, id={}, nextAttemptAt={}", key, rec.getId(), job.getNextAttemptAt());
                    continue;
                }

                // RUNNING + lease 설정
                job.setStatus(JobStatus.RUNNING);
                job.setLeaseUntil(Instant.now().plusSeconds(leaseSeconds));
                jobRepository.save(job);
                appendLog(jobId, "RUNNING", "lease set");

                // 핸들러 실행
                JobHandler handler = registry.get(job.getType());
                if (handler == null) throw new IllegalStateException("No handler for type=" + job.getType());
                handler.handle(jobIdStr, payload);

                // 성공 → SUCCEEDED
                job.setStatus(JobStatus.SUCCEEDED);
                job.setLeaseUntil(null);
                jobRepository.save(job);
                appendLog(jobId, "SUCCEEDED", null);

                ack(key, rec);
                log.info("[Worker] ACK key={}, id={}", key, rec.getId());

            } catch (Exception ex) {
                // 실패 → 재시도 or DLQ
                try {
                    Long jobId = Long.valueOf(jobIdStr);
                    Job job = jobRepository.findById(jobId).orElse(null);
                    if (job != null) {
                        int nextRetry = job.getRetryCount() + 1;

                        if (nextRetry > maxRetries) {
                            job.setStatus(JobStatus.DLQ);
                            job.setLeaseUntil(null);
                            jobRepository.save(job);
                            appendLog(jobId, "DLQ", ex.toString());

                            jobQueuePort.enqueueDlq(job.getType(), job.getPayloadJson(), job.getId().toString());

                            ack(key, rec);
                            log.warn("[Worker] DLQ jobId={}, redisId={}, err={}", job.getId(), rec.getId(), ex.toString());
                        } else {
                            Duration wait = Backoff.expJitter(
                                    job.getRetryCount(),
                                    baseBackoffMillis,
                                    backoffCapMillis,
                                    jitterRatio
                            );
                            job.setRetryCount(nextRetry);
                            job.setStatus(JobStatus.QUEUED);
                            job.setLeaseUntil(null);
                            job.setNextAttemptAt(Instant.now().plus(wait));
                            jobRepository.save(job);
                            appendLog(jobId, "RETRY", ex.toString());

                            ack(key, rec);
                            log.info("[Worker] reserved retry jobId={} after {} ms", job.getId(), wait.toMillis());
                        }
                    } else {
                        ack(key, rec);
                    }
                } catch (Exception nested) {
                    log.error("[Worker] retry-flow error: {}", nested.toString());
                    ack(key, rec);
                }
            }
        }
    }

    private void ack(String key, MapRecord<String, Object, Object> rec) {
        redis.opsForStream().acknowledge(key, groupName, rec.getId());
    }

    private void ensureGroup(String k, String g) {
        try { redis.opsForStream().createGroup(k, ReadOffset.latest(), g); }
        catch (Exception e) { /* BUSYGROUP 무시 */ }
    }

    private void appendLog(Long jobId, String event, String message) {
        logRepository.save(JobEventLog.builder()
                .jobId(jobId).eventType(event).message(message).build());
    }

}

