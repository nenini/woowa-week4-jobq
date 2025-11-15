package com.yerin.jobq.infra;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//@Profile("local")
public class LeaseReaper {

    private final JobRepository jobRepository;

    @Scheduled(fixedDelay = 2000)
    public void reap() {
        Instant now = Instant.now();

        List<Job> expired = jobRepository
                .findTop100ByStatusAndLeaseUntilLessThanEqualOrderByLeaseUntilAsc(
                        JobStatus.RUNNING, now);

        if (expired.isEmpty()) return;

        int handled = 0;
        for (Job j : expired) {
            try {
                int next = j.getRetryCount() + 1;
                j.setRetryCount(next);

                j.setStatus(JobStatus.QUEUED);
                j.setLeaseUntil(null);

                Duration wait = Backoff.expJitter(
                        next - 1,          // retryCount
                        1000L,             // baseBackoffMillis
                        60000L,            // backoffCapMillis
                        0.2                // jitterRatio
                );
                j.setNextAttemptAt(Instant.now().plus(wait));

                jobRepository.save(j);
                handled++;
            } catch (Exception e) {
                log.warn("[LeaseReaper] failed to revert jobId={}, err={}", j.getId(), e.toString());
            }
        }
        log.info("[LeaseReaper] reaped={} (RUNNING→QUEUED)", handled);
    }
}
