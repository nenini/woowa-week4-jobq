package com.yerin.jobq.controller;

import com.yerin.jobq.domain.JobStatus;
import com.yerin.jobq.repository.JobRepository;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final StringRedisTemplate redis;
    private final JobRepository jobRepository;

    @Value("${jobq.stream.prefix:jobq:stream}")
    private String streamPrefix;

    private final List<String> types = List.of("email_welcome");

    @GetMapping("/queue")
    public Map<String, Object> queue(@RequestHeader(value = "X-Admin-Token", required = true)
                                         @Parameter(description = "관리자 토큰", example = "test-admin-token")
                                         String adminToken) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String t : types) {
            String key = streamPrefix + ":" + t;
            Long xlen = redis.opsForStream().size(key);
            out.put(t, Map.of("xlen", xlen == null ? 0 : xlen));
        }
        return Map.of(
                "streams", out,
                "ts", Instant.now().toString()
        );
    }

    @GetMapping("/jobs")
    public Map<String, Long> jobCounts(@RequestHeader(value = "X-Admin-Token", required = true)
                                           @Parameter(description = "관리자 토큰", example = "test-admin-token")
                                           String adminToken) {
        return Map.of(
                "QUEUED", jobRepository.countByStatus(JobStatus.QUEUED),
                "RUNNING", jobRepository.countByStatus(JobStatus.RUNNING),
                "SUCCEEDED", jobRepository.countByStatus(JobStatus.SUCCEEDED),
                "DLQ", jobRepository.countByStatus(JobStatus.DLQ)
        );
    }
}