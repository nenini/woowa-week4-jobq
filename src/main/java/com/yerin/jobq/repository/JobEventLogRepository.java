package com.yerin.jobq.repository;

import com.yerin.jobq.domain.JobEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobEventLogRepository extends JpaRepository<JobEventLog, Long> { }

