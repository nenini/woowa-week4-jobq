package com.yerin.jobq.infra;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//@Profile("local")
public class DueJobEnqueuer {

    private final JobRepository jobRepository;
    private final JobQueuePort queuePort;

    @Transactional(readOnly = true)
    @Scheduled(fixedDelayString = "${jobq.worker.blockMillis:2000}")
    public void enqueueDue() {
        Instant now = Instant.now();

        List<Job> due = jobRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                JobStatus.QUEUED, now
        );

        for (Job j : due) {
            if (j.getQueuedAt() != null && j.getNextAttemptAt() != null
                    && !j.getQueuedAt().isBefore(j.getNextAttemptAt())) {
                continue;
            }

            try {
                queuePort.enqueueWithJobId(j.getType(), j.getPayloadJson(), null, j.getId().toString());
                j.setQueuedAt(now);
                jobRepository.save(j);
            } catch (Exception e) {
                log.warn("[DueJobEnqueuer] enqueue fail jobId={}, err={}", j.getId(), e.toString());
            }
        }
    }
}
