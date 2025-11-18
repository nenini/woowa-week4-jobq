package com.yerin.jobq.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job_event_log")
public class JobEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="job_id", nullable=false)
    private Long jobId;

    @Column(name="event_type", nullable=false, length=50)
    private String eventType;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name="ts", nullable=false)
    private Instant ts;

    @PrePersist void pre() { if (ts == null) ts = Instant.now(); }
}