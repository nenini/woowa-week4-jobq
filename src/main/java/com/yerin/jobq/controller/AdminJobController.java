package com.yerin.jobq.controller;

import com.yerin.jobq.domain.Job;
import com.yerin.jobq.dto.JobResponse;
import com.yerin.jobq.global.dto.DataResponse;
import com.yerin.jobq.service.AdminJobService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/jobs")
public class AdminJobController {
    private final AdminJobService adminJobService;

    @PostMapping("/{id}/replay")
    public ResponseEntity<DataResponse<JobResponse>> replay(@PathVariable Long id,
                                                            @RequestHeader(value = "X-Admin-Token", required = true)
                                                            @Parameter(description = "관리자 토큰", example = "test-admin-token")
                                                            String adminToken) {
        Job job = adminJobService.replay(id);
        return ResponseEntity.ok(DataResponse.from(JobResponse.from(job)));
    }
}

