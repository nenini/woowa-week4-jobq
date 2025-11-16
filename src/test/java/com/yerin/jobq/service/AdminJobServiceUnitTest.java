package com.yerin.jobq.service;


import com.yerin.jobq.domain.Job;
import com.yerin.jobq.domain.JobQueuePort;
import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.checkerframework.checker.interning.qual.InternedDistinct;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("관리자 Job 재생성(Replay) 서비스 테스트")
public class AdminJobServiceUnitTest {
    JobRepository repo = mock(JobRepository.class);
    JobQueuePort port = mock(JobQueuePort.class);
    AdminJobService sut = new AdminJobService(repo, port);

    @Test
    @DisplayName("DLQ일 때만 재생성 허용")
    void replay_only_when_DLQ() {
        var job = Job.builder().id(99L).type("email_welcome").status(JobStatus.DLQ)
                .payloadJson("{\"userId\":777}").build();
        when(repo.findById(99L)).thenReturn(Optional.of(job));

        var res = sut.replay(99L);

        assertThat(res.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(res.getNextAttemptAt()).isAfter(Instant.now().minusSeconds(1));
        verify(port).enqueueWithJobId(eq("email_welcome"), any(), isNull(), eq("99"));
    }

    @Test
    @DisplayName("DLQ가 아니면 재생성 거부")
    void replay_non_DLQ_is_error() {
        var job = Job.builder().id(1L).type("email_welcome").status(JobStatus.QUEUED).build();
        when(repo.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> sut.replay(1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
