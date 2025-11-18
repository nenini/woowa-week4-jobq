package com.yerin.jobq.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "job")
@DynamicUpdate
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String type;

    @Column(name="payload_json", nullable=false,columnDefinition = "text")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=30)
    private JobStatus status;

    @Column(name="retry_count", nullable=false)
    private int retryCount;

    @Column(name="lease_until")
    private Instant leaseUntil;

    @Column(name="next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    @Column(name="queued_at")
    private Instant queuedAt;


    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = JobStatus.QUEUED;
        if (nextAttemptAt == null) nextAttemptAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
