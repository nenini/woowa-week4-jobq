package com.yerin.jobq.domain;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class JobqMetrics {

    private final MeterRegistry registry;

    private final Counter jobCreated;
    private final Counter jobSucceeded;
    private final Counter jobFailed;
    private final Counter jobRetried;
    private final Counter jobDlq;

    public JobqMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.jobCreated   = Counter.builder("jobq_jobs_created_total")
                .description("jobs created").register(registry);
        this.jobSucceeded = Counter.builder("jobq_jobs_succeeded_total")
                .description("jobs succeeded").register(registry);
        this.jobFailed    = Counter.builder("jobq_jobs_failed_total")
                .description("jobs failed (handler thrown)").register(registry);
        this.jobRetried   = Counter.builder("jobq_jobs_retried_total")
                .description("jobs scheduled for retry").register(registry);
        this.jobDlq       = Counter.builder("jobq_jobs_dlq_total")
                .description("jobs moved to DLQ").register(registry);
    }

    public void incCreated()   { jobCreated.increment(); }
    public void incSucceeded() { jobSucceeded.increment(); }
    public void incFailed()    { jobFailed.increment(); }
    public void incRetried()   { jobRetried.increment(); }
    public void incDlq()       { jobDlq.increment(); }

    // 타입 태그가 붙은 타이머 제공
    public Timer handlerTimer(String type) {
        return Timer.builder("jobq_handler_duration_seconds")
                .description("handler duration by type")
                .tag("type", type)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(registry);
    }
}