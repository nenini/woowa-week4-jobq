package com.yerin.jobq.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yerin.jobq.domain.Job;
import com.yerin.jobq.dto.JobResponse;
import com.yerin.jobq.global.dto.DataResponse;
import com.yerin.jobq.global.exception.AppException;
import com.yerin.jobq.global.exception.code.JobErrorCode;
import com.yerin.jobq.repository.JobRepository;
import com.yerin.jobq.service.EnqueueJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final EnqueueJobService enqueueJobService;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;

    @PostMapping("/{type}")
    public ResponseEntity<DataResponse<Map<String, String>>> enqueue(
            @PathVariable String type,
            @RequestBody Map<String, Object> body
    ) throws JsonProcessingException {
        String idempotencyKey = (String) body.get("idempotencyKey");
        String payloadJson = objectMapper.writeValueAsString(body);

        String jobId = enqueueJobService.enqueue(type, payloadJson, idempotencyKey);
        Map<String, String> responseMap = Map.of("jobId", jobId);

        return ResponseEntity.ok(DataResponse.from(responseMap));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<JobResponse>> get(@PathVariable Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new AppException(JobErrorCode.JOB_NOT_FOUND));

        JobResponse response = JobResponse.from(job);
        return ResponseEntity.ok(DataResponse.from(response));
    }
}
