package com.yerin.jobq.service;

import com.yerin.jobq.domain.*;
import com.yerin.jobq.repository.JobIdempotencyRepository;
import com.yerin.jobq.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EnqueueJobService 단위 테스트")
public class EnqueueJobServiceUnitTest {
    JobRepository jobRepo = mock(JobRepository.class);
    JobIdempotencyRepository idemRepo = mock(JobIdempotencyRepository.class);
    JobQueuePort port = mock(JobQueuePort.class);
    JobqMetrics metrics = mock(JobqMetrics.class);

    EnqueueJobService sut = new EnqueueJobService(port, jobRepo, idemRepo, metrics);

    @Test
    @DisplayName("작업 생성 시 필드 세팅 및 큐 포트 호출")
    void create_sets_fields_and_calls_port() {
        // given
        when(idemRepo.findByIdempotencyKey("k")).thenReturn(java.util.Optional.empty());
        Job saved = Job.builder().id(1L).type("email_welcome").build();
        when(jobRepo.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0, Job.class);
            if (j.getId() == null) j.setId(1L);
            return j;
        });

        // when
        String id = sut.enqueue("email_welcome", "{\"userId\":42}", "k");

        // then
        assertThat(id).isEqualTo("1");

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepo, times(2)).save(captor.capture());

        Job afterQueued = captor.getValue(); // 마지막 save
        assertThat(afterQueued.getQueuedAt()).isNotNull();
        assertThat(afterQueued.getStatus()).isEqualTo(JobStatus.QUEUED);

        verify(port).enqueueWithJobId("email_welcome", "{\"userId\":42}", "k", "1");
        verify(metrics).incCreated();
    }

    @Test
    @DisplayName("멱등키가 이미 존재하면 기존 jobId 반환하고 새로 저장/큐잉하지 않음")
    void idempotent_returns_existing_jobId() {
        var idem = JobIdempotency.builder()
                .idempotencyKey("i-k")
                .jobId(9L)
                .createdAt(Instant.now())
                .build();
        when(idemRepo.findByIdempotencyKey("i-k")).thenReturn(Optional.of(idem));

        String id = sut.enqueue("email_welcome", "{}", "i-k");
        assertThat(id).isEqualTo("9");

        verify(jobRepo, never()).save(any());
        verify(port, never()).enqueueWithJobId(any(), any(), any(), any());
        verify(metrics, never()).incCreated();
    }

}
