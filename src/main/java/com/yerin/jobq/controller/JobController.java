package com.yerin.jobq.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        // Day2: JPA 조회로 상태 반환 예정
        return ResponseEntity.ok(Map.of("id", id, "status", "TBD"));
    }
}
