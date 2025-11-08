create table if not exists job (
                                   id               bigserial primary key,
                                   type             varchar(100) not null,
    payload_json     text not null,
    status           varchar(30) not null,
    retry_count      int not null default 0,
    lease_until      timestamptz,
    next_attempt_at  timestamptz,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
    );

create table if not exists idempotency (
                                           key              varchar(255) primary key,
    job_id           bigint not null references job(id),
    created_at       timestamptz not null default now()
    );

create table if not exists job_event_log (
                                             id         bigserial primary key,
                                             job_id     bigint not null references job(id),
    event_type varchar(50) not null,
    message    text,
    ts         timestamptz not null default now()
    );

create index if not exists idx_job_status_next_attempt
    on job(status, next_attempt_at);
