package com.yerin.jobq.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yerin.jobq.repository.JobRepository;
import com.yerin.jobq.service.EnqueueJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final EnqueueJobService enqueueJobService;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;

    @PostMapping("/{type}")
    public ResponseEntity<Map<String, Object>> enqueue(
            @PathVariable String type,
            @RequestBody Map<String, Object> body
    ) throws JsonProcessingException {
        String idempotencyKey = (String) body.getOrDefault("idempotencyKey", null);
        String payloadJson = objectMapper.writeValueAsString(body);

        String jobId = enqueueJobService.enqueue(type, payloadJson, idempotencyKey);
        return ResponseEntity.ok(Map.of("jobId", jobId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return jobRepository.findById(id)
                .<ResponseEntity<?>>map(j -> ResponseEntity.ok(Map.of(
                        "id", j.getId(),
                        "type", j.getType(),
                        "status", j.getStatus(),
                        "retryCount", j.getRetryCount(),
                        "nextAttemptAt", j.getNextAttemptAt(),
                        "leaseUntil", j.getLeaseUntil(),
                        "createdAt", j.getCreatedAt(),
                        "updatedAt", j.getUpdatedAt()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


}
