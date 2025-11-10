package com.yerin.jobq.repository;

import com.yerin.jobq.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> { }
