package com.yerin.jobq.domain;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@DisplayName("메트릭 카운터/타이머 집계 테스트")
public class JobqMetricsTest {

    @Test
    @DisplayName("카운터 및 타이머 동작 검증")
    void counters_and_timer_work() {
        var reg = new SimpleMeterRegistry();
        var m = new JobqMetrics(reg);

        m.incCreated();
        m.incFailed();
        m.incRetried();
        m.incDlq();
        m.incSucceeded();

        assertThat(reg.find("jobq_jobs_created_total").counter().count()).isEqualTo(1.0);
        assertThat(reg.find("jobq_jobs_failed_total").counter().count()).isEqualTo(1.0);
        assertThat(reg.find("jobq_jobs_retried_total").counter().count()).isEqualTo(1.0);
        assertThat(reg.find("jobq_jobs_dlq_total").counter().count()).isEqualTo(1.0);
        assertThat(reg.find("jobq_jobs_succeeded_total").counter().count()).isEqualTo(1.0);

        var timer = reg.find("jobq_handler_duration_seconds")
                .tag("type", "email_welcome")
                .timer();

        if (timer == null) {
            m.handlerTimer("email_welcome");
            timer = reg.find("jobq_handler_duration_seconds")
                    .tag("type", "email_welcome")
                    .timer();
        }

        m.handlerTimer("email_welcome").record(java.time.Duration.ofMillis(5));

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.SECONDS)).isGreaterThan(0.0);
    }
}
