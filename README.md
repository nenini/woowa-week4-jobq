# woowa-week4-jobq

우테코 프리코스 4주차 오픈미션 – **Redis Streams 기반 경량 Job Queue**  
Spring Boot 3.3.1 / Postgres / Redis / Micrometer-Prometheus

## 프로젝트 목적
- At-least-once 소비 환경에서 **멱등성 보장**, **재시도/백오프/지터**, **DLQ 격리**, 운영 가시성(메트릭)을 갖춘 간단한 잡큐를 학습/구현

## 핵심 기능
- **Enqueue API**: 작업 생성(+idempotencyKey) → Redis Stream publish
- **Worker**: 예약 시간 도래 시 처리, 실패 시 **지수 백오프 + 지터** 후 재시도
- **DLQ**: 재시도 한도 초과 시 DLQ 스트림으로 격리
- **관리 API**: DLQ → 재적재(Replay)
- **조회 API**: `/jobs/{id}` (상태, retryCount, nextAttemptAt, queuedAt 등)
- **메트릭**: 생성/성공/실패/재시도/DLQ 카운터, 핸들러 처리시간 summary

## API 요약
- POST /jobs/email_welcome : {"userId":42,"idempotencyKey":"..."}
- GET /jobs/{id} : 상태 조회
- POST /admin/jobs/{id}/replay : DLQ 재적재(헤더 X-Admin-Token 필요)

## 진행 기록
1. 기본 모델/엔큐/워커 뼈대, 단일 타입 email_welcome

2. 재시도/백오프/지터, 예약 처리

3. Lease(고아 방지), Due 재큐어(LeaseReaper/DueJobEnqueuer), 이벤트 로그

4. 멱등성(idempotencyKey), DLQ 스트림, 조회 API 보강(queuedAt)

5. 관리 API(Replay), 운영 메트릭(Micrometer/Prometheus)

## 실행 방법
```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'