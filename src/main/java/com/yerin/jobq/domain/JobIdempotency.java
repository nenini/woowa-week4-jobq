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
@Table(name = "job_idempotency", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idem_key", columnNames = "idempotency_key")
})
public class JobIdempotency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="idempotency_key", nullable=false)
    private String idempotencyKey;

    @Column(name="job_id", nullable=false)
    private Long jobId;

    @Column(name="created_at", nullable=false)
    private Instant createdAt = Instant.now();

    public JobIdempotency(String s, long l, Instant now) {
    }
}

