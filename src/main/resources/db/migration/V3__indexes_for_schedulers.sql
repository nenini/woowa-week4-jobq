CREATE INDEX IF NOT EXISTS idx_job_status_next_attempt
    ON job (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_job_status_lease_until
    ON job (status, lease_until);

CREATE INDEX IF NOT EXISTS idx_job_queued_at
    ON job (queued_at);
