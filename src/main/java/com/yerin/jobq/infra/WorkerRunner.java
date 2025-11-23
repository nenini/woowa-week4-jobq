package com.yerin.jobq.infra;

import com.yerin.jobq.application.JobHandler;
import com.yerin.jobq.application.JobHandlerRegistry;
import com.yerin.jobq.domain.*;
import com.yerin.jobq.repository.JobEventLogRepository;
import com.yerin.jobq.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
//@Profile("local")
public class WorkerRunner {
    private final StringRedisTemplate redis;
    private final JobRepository jobRepository;
    private final JobEventLogRepository logRepository;
    private final JobHandlerRegistry registry;
    private final JobQueuePort jobQueuePort;
    private final JobqMetrics metrics;

    private final PlatformTransactionManager txManager;
    private TransactionTemplate tx;

    @PostConstruct
    void initTx() {
        this.tx = new TransactionTemplate(txManager);
    }

    @Value("${jobq.stream.prefix:jobq:stream}")
    private String streamPrefix;

    @Value("${jobq.stream.group:jobq:cg}")
    private String groupName;

    @Value("${jobq.worker.batchSize:10}")
    private long batchSize;

    @Value("${jobq.worker.blockMillis:2000}")
    private long blockMillis;

    @Value("${jobq.worker.leaseSeconds:30}")
    private long leaseSeconds;

    @Value("${jobq.worker.concurrency:1}")
    private int concurrency;

    @Value("${jobq.retry.maxRetries:5}")
    private int maxRetries;

    @Value("${jobq.retry.baseBackoffMillis:1000}")
    private long baseBackoffMillis;

    @Value("${jobq.retry.backoffCapMillis:60000}")
    private long backoffCapMillis;

    @Value("${jobq.retry.jitterRatio:0.2}")
    private double jitterRatio;

    private ExecutorService workers;
    private volatile boolean grouped = false; // 그룹 준비 1회 보장

    private String streamKey(String type) {
        return streamPrefix + ":" + type;
    }


    @PostConstruct
    void startWorkers() {
        final String type = "email_welcome";
        ensureGroupOnce(type);

        workers = Executors.newFixedThreadPool(concurrency);
        for (int i = 0; i < concurrency; i++) {
            final int idx = i;
            final String consumer = WorkerId.consumerName() + "-" + idx;
            workers.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        pollOnce(type, consumer);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        log.warn("[Worker] poll loop error: {}", e.toString());
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            });
        }
        log.info("[Worker] started {} consumers for type=email_welcome", concurrency);
    }

    @PreDestroy
    void stopWorkers() {
        if (workers != null) {
            workers.shutdownNow();
        }
    }


    public void pollOnce(String type, String consumer) throws InterruptedException {
        final String key = streamKey(type);

        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(groupName, consumer),
                StreamReadOptions.empty().count(batchSize).block(Duration.ofMillis(blockMillis)),
                StreamOffset.create(key, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> rec : records) {
            if (rec.getValue().containsKey("bootstrap") || rec.getValue().get("jobId") == null) {
                ack(key, rec);
                log.debug("[Worker] skip bootstrap/invalid rec id={}", rec.getId());
                continue;
            }

            final String jobIdStr = String.valueOf(rec.getValue().get("jobId"));
            final String payload = (String) rec.getValue().get("payload");

            try {
                final Long jobId = Long.valueOf(jobIdStr);

                // 최소 조회(타입/재시도/nextAttemptAt 등 확인용)
                Job job = jobRepository.findById(jobId).orElse(null);
                if (job == null) { ack(key, rec); continue; }

                // 아직 due가 아니면 ACK
                if (job.getNextAttemptAt() != null && job.getNextAttemptAt().isAfter(Instant.now())) {
                    ack(key, rec);
                    log.info("[Worker] skip(not-due) key={}, id={}, nextAttemptAt={}", key, rec.getId(), job.getNextAttemptAt());
                    continue;
                }

                Integer grabbed = tx.execute(status ->
                        jobRepository.updateStatusAndLeaseIf(
                                jobId, JobStatus.QUEUED, JobStatus.RUNNING, Instant.now().plusSeconds(leaseSeconds)
                        ));
                if (grabbed == null || grabbed == 0) {
                    ack(key, rec);
                    log.debug("[Worker] lost race jobId={}, redisId={}", jobId, rec.getId());
                    continue;
                }

                appendLog(jobId, "RUNNING", "lease set");

                // 핸들러 실행 + 타이머
                long start = System.nanoTime();
                try {
                    JobHandler handler = registry.get(job.getType());
                    if (handler == null) throw new IllegalStateException("No handler for type=" + job.getType());
                    handler.handle(jobIdStr, payload);
                } finally {
                    metrics.handlerTimer(job.getType()).record(Duration.ofNanos(System.nanoTime() - start));
                }

                // 성공 전이 RUNNING -> SUCCEEDED (원자)
                tx.execute(status -> jobRepository.succeedIfRunning(jobId));
                appendLog(jobId, "SUCCEEDED", null);
                metrics.incSucceeded();
                ack(key, rec);
                log.info("[Worker] ACK key={}, id={}", key, rec.getId());

            } catch (Exception ex) {
                metrics.incFailed();
                try {
                    final Long jobId = Long.valueOf(jobIdStr);

                    // 재시도 횟수/타입 확인은 한번 더 읽어서 판단(읽기 1회는 괜찮음)
                    Job cur = jobRepository.findById(jobId).orElse(null);
                    if (cur == null) { ack(key, rec); continue; }

                    int nextRetry = cur.getRetryCount() + 1;
                    if (nextRetry > maxRetries) {
                        // DLQ 전이 RUNNING -> DLQ (원자)
                        tx.execute(status -> jobRepository.dlqIfRunning(jobId));
                        appendLog(jobId, "DLQ", ex.toString());
                        metrics.incDlq();

                        jobQueuePort.enqueueDlq(cur.getType(), cur.getPayloadJson(), String.valueOf(jobId));

                        ack(key, rec);
                        log.warn("[Worker] DLQ jobId={}, redisId={}, err={}", jobId, rec.getId(), ex.toString());
                    } else {
                        // 재시도 예약 RUNNING -> QUEUED (원자)
                        Duration wait = Backoff.expJitter(
                                cur.getRetryCount(), baseBackoffMillis, backoffCapMillis, jitterRatio
                        );
                        Integer updated = tx.execute(status -> jobRepository.updateRetryIf(
                                jobId, JobStatus.RUNNING, JobStatus.QUEUED, nextRetry, Instant.now().plus(wait)
                        ));

                        if (updated == null || updated == 0) {
                            // 레이스로 상태 바뀜 → 로그만 남기고 ACK
                            log.warn("[Worker] retry update lost race jobId={}", jobId);
                        } else {
                            appendLog(jobId, "RETRY", ex.toString());
                            metrics.incRetried();
                            log.info("[Worker] reserved retry jobId={} after {} ms", jobId, wait.toMillis());
                        }

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

    private void ensureGroupOnce(String type) {
        if (grouped) return;
        synchronized (this) {
            if (grouped) return;

            String key = streamKey(type);
            try {
                RecordId rid = redis.opsForStream().add(
                        StreamRecords.mapBacked(Map.of("bootstrap", "1")).withStreamKey(key)
                );
                redis.opsForStream().createGroup(key, ReadOffset.latest(), groupName);
                redis.opsForStream().delete(key, rid);

                log.info("[RedisStream] group prepared key={}, group={}, rec={}", key, groupName, rid);
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (!(msg.contains("BUSYGROUP") || msg.contains("already exists"))) {
                    log.warn("[RedisStream] createGroup ignored key={}, group={}, cause={}", key, groupName, msg);
                }
            }
            grouped = true;
        }
    }

    private void appendLog(Long jobId, String event, String message) {
        logRepository.save(
                com.yerin.jobq.domain.JobEventLog.builder()
                        .jobId(jobId).eventType(event).message(message).build()
        );
    }

}

