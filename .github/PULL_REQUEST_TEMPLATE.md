## 개요
- 이 PR의 목적을 한 줄로 설명

## 주요 변경


## 스크린샷/로그(선택)


## 체크리스트
- [ ] 관련 이슈에 연결 (`Closes #번호`)
- [ ] 기능 플래그/프로필 반영
- [ ] 테스트/로컬 수동 검증 완료
- [ ] 문서/README 갱신

## 테스트 방법
```bash
# 예시
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
curl http://localhost:8080/actuator/health
