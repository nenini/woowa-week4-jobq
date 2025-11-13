CREATE TABLE IF NOT EXISTS job_idempotency (
                                               id                BIGSERIAL PRIMARY KEY,
                                               idempotency_key   TEXT NOT NULL UNIQUE,
                                               job_id            BIGINT NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    created_at        timestamptz NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS email_welcome_processed (
                                                       id         BIGSERIAL PRIMARY KEY,
                                                       user_id    BIGINT NOT NULL UNIQUE,
                                                       created_at timestamptz NOT NULL DEFAULT now()
    );
