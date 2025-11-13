package com.yerin.jobq.controller;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.dto.JobResponse;
import com.yerin.jobq.service.AdminJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/jobs")
public class AdminJobController {
    private final AdminJobService adminJobService;

    @PostMapping("/{id}/replay")
    public ResponseEntity<JobResponse> replay(@PathVariable Long id) {
        Job job = adminJobService.replay(id);
        return ResponseEntity.ok(JobResponse.from(job));
    }
}

